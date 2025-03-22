import time
import concurrent.futures
import numpy as np
from sqlalchemy import text
from app.config.database import SessionLocal
from app.utils.haversine import haversine
from sklearn.metrics.pairwise import cosine_similarity

def distance_score(distance):
    """
    두 지점 사이의 거리를 기반으로 점수를 계산합니다.
    공식: 1 / (1 + distance)
    """
    return 1 / (1 + distance)

def get_poi_by_category(category: str):
    """
    각 카테고리별 POI 데이터를 DB에서 조회합니다.
    반환 예시: [{"latitude": float, "longitude": float}, ...]
    """
    session = SessionLocal()
    try:
        if category == "transport":
            query = text("SELECT latitude, longitude FROM transport")
        elif category == "restaurant":
            query = text("SELECT latitude, longitude FROM restaurant")
        elif category == "health":
            query = text("SELECT latitude, longitude FROM health")
        elif category == "convenience":
            query = text("SELECT latitude, longitude FROM convenience")
        elif category == "cafe":
            query = text("SELECT latitude, longitude FROM cafe")
        elif category == "chicken":
            query = text("SELECT latitude, longitude FROM chicken")
        elif category == "leisure":
            query = text("SELECT latitude, longitude FROM leisure")
        else:
            return []
        
        result = session.execute(query).fetchall()
        poi_list = [
            {"latitude": float(row._mapping["latitude"]), "longitude": float(row._mapping["longitude"])}
            for row in result
        ]
        return poi_list
    finally:
        session.close()

def calculate_category_score(property_lat, property_lon, poi_list, radius=1.0, alpha=0.5, beta=0.5):
    """
    주어진 POI 리스트에 대해 카테고리 점수를 계산합니다.
      - radius: 고려 반경 (1km)
      - alpha: POI 개수에 적용할 가중치
      - beta: 가장 가까운 POI 거리 기반 점수에 적용할 가중치
    반환값: (POI 개수, 거리 기반 점수, 최종 카테고리 점수)
    """
    count = 0
    min_distance = float('inf')
    for poi in poi_list:
        d = haversine(property_lat, property_lon, poi["latitude"], poi["longitude"])
        if d <= radius:
            count += 1
            if d < min_distance:
                min_distance = d
    if count == 0:
        min_distance = radius
    dist_score = distance_score(min_distance)
    category_score = alpha * count + beta * dist_score
    return count, dist_score, category_score

def compute_property_score(property_data: dict):
    """
    매물(property_data: latitude, longitude 포함)에 대해 각 카테고리별 점수를 계산합니다.
    반환 예시:
      {
          "transport_count": 3, "transport_score": 1.5,
          "restaurant_count": 2, "restaurant_score": 1.2,
          ...
      }
    """
    lat = property_data["latitude"]
    lon = property_data["longitude"]

    results = {}
    categories = {
        "transport": {"alpha": 0.5, "beta": 0.5},
        "restaurant": {"alpha": 0.5, "beta": 0.5},
        "health": {"alpha": 0.6, "beta": 0.4},
        "convenience": {"alpha": 0.5, "beta": 0.5},
        "cafe": {"alpha": 0.4, "beta": 0.6},
        "chicken": {"alpha": 0.4, "beta": 0.6},
        "leisure": {"alpha": 0.5, "beta": 0.5}
    }
    
    for cat, weight_params in categories.items():
        poi_list = get_poi_by_category(cat)
        count, _, cat_score = calculate_category_score(
            lat, lon, poi_list,
            radius=1.0,
            alpha=weight_params["alpha"],
            beta=weight_params["beta"]
        )
        results[f"{cat}_count"] = count
        results[f"{cat}_score"] = cat_score
    return results

def update_property_score(property_id: int, score_data: dict):
    """
    property_score 테이블에서 해당 매물(property_id)의 점수를 업데이트합니다.
    기록이 없으면 새로 삽입합니다.
    """
    session = SessionLocal()
    try:
        stmt = text(
            "UPDATE property_score SET "
            "transport_count = :transport_count, transport_score = :transport_score, "
            "restaurant_count = :restaurant_count, restaurant_score = :restaurant_score, "
            "health_count = :health_count, health_score = :health_score, "
            "convenience_count = :convenience_count, convenience_score = :convenience_score, "
            "cafe_count = :cafe_count, cafe_score = :cafe_score, "
            "chicken_count = :chicken_count, chicken_score = :chicken_score, "
            "leisure_count = :leisure_count, leisure_score = :leisure_score "
            "WHERE property_id = :property_id"
        )
        params = {"property_id": property_id, **score_data}
        result = session.execute(stmt, params)
        if result.rowcount == 0:
            insert_stmt = text(
                "INSERT INTO property_score (property_id, transport_count, transport_score, "
                "restaurant_count, restaurant_score, health_count, health_score, "
                "convenience_count, convenience_score, cafe_count, cafe_score, "
                "chicken_count, chicken_score, leisure_count, leisure_score) "
                "VALUES (:property_id, :transport_count, :transport_score, "
                ":restaurant_count, :restaurant_score, :health_count, :health_score, "
                ":convenience_count, :convenience_score, :cafe_count, :cafe_score, "
                ":chicken_count, :chicken_score, :leisure_count, :leisure_score)"
            )
            session.execute(insert_stmt, params)
        session.commit()
        return True
    except Exception as e:
        session.rollback()
        print("Error updating property score:", e)
        return False
    finally:
        session.close()

def process_property(row):
    """
    단일 매물(row)에 대해 점수를 계산하고 DB에 업데이트합니다.
    """
    try:
        prop_id = row._mapping["property_id"]
        lat = row._mapping["latitude"]
        lon = row._mapping["longitude"]
        score_data = compute_property_score({"latitude": lat, "longitude": lon})
        update_property_score(prop_id, score_data)
    except Exception as e:
        print(f"Error processing property_id {row._mapping['property_id']}: {e}")

# === 검증을 위한 세 가지 방식 ===

def recalculate_all_scores_no_batch():
    """
    [비배치 방식] 전체 데이터를 한 번에 로드한 후 단일 스레드로 순차 처리합니다.
    메모리 사용은 높지만, 배치 처리 미도입 시 처리 시간을 측정할 수 있습니다.
    """
    session = SessionLocal()
    try:
        rows = session.execute(
            text("SELECT property_id, latitude, longitude FROM property ORDER BY property_id")
        ).fetchall()
    finally:
        session.close()
    total_processed = 0
    for row in rows:
        process_property(row)
        total_processed += 1
        if total_processed % 1000 == 0:
            print(f"No-batch: Processed {total_processed} properties...")
    return total_processed

def recalculate_all_scores_single(limit=20000, batch_size=1000):
    """
    [단일 스레드 배치 방식] 데이터를 배치로 조회하여 단일 스레드로 처리합니다.
    테스트용으로 limit만큼 처리합니다.
    """
    session = SessionLocal()
    total_processed = 0
    try:
        offset = 0
        while total_processed < limit:
            query = text(
                "SELECT property_id, latitude, longitude FROM property "
                "ORDER BY property_id LIMIT :limit OFFSET :offset"
            )
            rows = session.execute(query, {"limit": batch_size, "offset": offset}).fetchall()
            if not rows:
                break
            for row in rows:
                process_property(row)
            processed_count = len(rows)
            total_processed += processed_count
            offset += batch_size
            print(f"Single-threaded batch: Processed {total_processed} properties...")
    except Exception as e:
        print("Error in single-threaded processing:", e)
    finally:
        session.close()
    return total_processed

def recalculate_all_scores_batch(batch_size=1000, max_workers=8, limit=None):
    """
    [멀티스레드 배치 방식] 데이터를 배치로 조회하여 각 배치를 ThreadPoolExecutor를 사용해 병렬 처리합니다.
    limit 파라미터가 None이면 property 테이블의 전체 데이터를 처리합니다.
    """
    session = SessionLocal()
    total_processed = 0
    try:
        # limit이 지정되지 않은 경우, 전체 row 수를 가져옵니다.
        if limit is None:
            total_count = session.execute(text("SELECT COUNT(*) FROM property")).scalar()
            limit = total_count
            print(f"Total properties to process: {limit}")
        
        offset = 0
        while total_processed < limit:
            query = text(
                "SELECT property_id, latitude, longitude FROM property "
                "ORDER BY property_id LIMIT :batch_size OFFSET :offset"
            )
            rows = session.execute(query, {"batch_size": batch_size, "offset": offset}).fetchall()
            if not rows:
                break

            with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
                futures = [executor.submit(process_property, row) for row in rows]
                concurrent.futures.wait(futures)

            processed_count = len(rows)
            total_processed += processed_count
            offset += batch_size
            print(f"Multi-threaded batch: Processed {total_processed} properties...")
    except Exception as e:
        print("Error in batch processing:", e)
    finally:
        session.close()
    return total_processed


# 이 함수들은 FastAPI 엔드포인트나 백그라운드 작업으로 호출하여 성능 테스트에 활용할 수 있습니다.
# 예시: 엔드포인트에서 각 방식을 호출하고, 소요 시간을 비교해 결과를 반환할 수 있습니다.
