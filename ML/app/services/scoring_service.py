# score_calculation.py
import time
import concurrent.futures
import numpy as np
import threading
from sqlalchemy import text
from app.config.database import SessionLocal
from app.utils.haversine import haversine

# 전역 캐시: 각 카테고리별 POI 데이터를 한 번만 로드하여 재사용
POI_CACHE = {}
# 캐시 초기화 락
CACHE_LOCK = threading.Lock()

def get_poi_by_category(category: str, session=None):
    """
    각 카테고리별 POI 데이터를 DB에서 조회한 후 전역 캐시에 저장합니다.
    반환 예시: [{"latitude": float, "longitude": float}, ...]
    스레드 안전을 위해 락 사용
    """
    global POI_CACHE
    
    # 캐시에 있으면 바로 반환
    if category in POI_CACHE:
        return POI_CACHE[category]
    
    # 캐시에 없으면 락 획득 후 다시 확인 (Double-checked locking)
    with CACHE_LOCK:
        if category in POI_CACHE:  # 다른 스레드가 이미 캐시를 채웠는지 확인
            return POI_CACHE[category]
        
        close_session = False
        if session is None:
            session = SessionLocal()
            close_session = True
        
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
                POI_CACHE[category] = []
                return []
            
            result = session.execute(query).fetchall()
            poi_list = [
                {"latitude": float(row._mapping["latitude"]), "longitude": float(row._mapping["longitude"])}
                for row in result
            ]
            POI_CACHE[category] = poi_list
            return poi_list
        finally:
            if close_session and session:
                session.close()

def initialize_poi_cache():
    """
    애플리케이션 시작 시 모든 카테고리에 대한 POI 캐시를 미리 초기화합니다.
    """
    categories = ["transport", "restaurant", "health", "convenience", "cafe", "chicken", "leisure"]
    session = SessionLocal()
    try:
        for category in categories:
            get_poi_by_category(category, session=session)
        print("POI cache initialized for all categories.")
    finally:
        session.close()

def distance_score(distance):
    """
    두 지점 사이의 거리를 기반으로 점수를 계산합니다.
    공식: 1 / (1 + distance)
    """
    return 1 / (1 + distance)

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

def compute_property_score(property_data: dict, session=None):
    """
    매물(property_data: latitude, longitude 포함)에 대해 각 카테고리별 점수를 계산합니다.
    세션을 인자로 받아 POI 데이터 조회 시 재사용합니다.
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
        poi_list = get_poi_by_category(cat, session=session)  # 세션 재사용
        count, _, cat_score = calculate_category_score(
            lat, lon, poi_list,
            radius=1.0,
            alpha=weight_params["alpha"],
            beta=weight_params["beta"]
        )
        results[f"{cat}_count"] = count
        results[f"{cat}_score"] = cat_score
    return results

def update_property_score_optimized(property_id: int, score_data: dict, session=None, max_retries=3):
    """
    외부 세션(session)이 전달되면 이를 사용하여 업데이트하고, 그렇지 않으면 새 세션을 생성합니다.
    데드락 발생 시 최대 max_retries번 재시도합니다.
    """
    retries = 0
    local_session = None
    
    while retries < max_retries:
        close_session = False
        try:
            # 세션 관리 개선
            if session is not None:
                # 외부에서 전달받은 세션 사용
                current_session = session
            else:
                # 내부에서 새 세션 생성
                if local_session is None or local_session.is_active is False:
                    local_session = SessionLocal()
                current_session = local_session
                close_session = True
            
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
            result = current_session.execute(stmt, params)
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
                current_session.execute(insert_stmt, params)
            if close_session:
                current_session.commit()
            return True
        except Exception as e:
            if current_session.is_active:
                current_session.rollback()
            
            if "Deadlock found" in str(e):
                retries += 1
                print(f"Deadlock encountered for property_id {property_id}, retry {retries}/{max_retries}")
                time.sleep(1)  # 잠시 대기 후 재시도
                continue
            else:
                print(f"Error updating property score for ID {property_id}: {e}")
                return False
        finally:
            # 내부에서 생성한 세션만 닫음
            if close_session and current_session.is_active:
                current_session.close()
    
    # 모든 재시도 실패 후 내부에서 생성한 세션 정리
    if local_session and local_session.is_active:
        local_session.close()
    
    print(f"Max retries reached for property_id {property_id}")
    return False

def process_property(row, session=None):
    """
    단일 매물(row)에 대해 점수를 계산하고 DB에 업데이트합니다.
    세션을 인자로 받아 재사용하도록 수정
    """
    close_session = False
    if session is None:
        session = SessionLocal()
        close_session = True
    
    try:
        prop_id = row._mapping["property_id"]
        lat = row._mapping["latitude"]
        lon = row._mapping["longitude"]
        score_data = compute_property_score({"latitude": lat, "longitude": lon}, session=session)
        return update_property_score_optimized(prop_id, score_data, session=session)
    except Exception as e:
        if session.is_active:
            session.rollback()
        print(f"Error processing property_id {row._mapping['property_id']}: {e}")
        return False
    finally:
        if close_session and session.is_active:
            session.close()

def process_property_batch(rows):
    """
    배치 단위로 처리하는 함수
    하나의 세션으로 배치 내 모든 매물 처리
    """
    session = SessionLocal()
    success_count = 0
    try:
        for row in rows:
            if process_property(row, session=session):
                success_count += 1
        session.commit()
        return success_count
    except Exception as e:
        session.rollback()
        print(f"Error processing batch: {e}")
        return success_count
    finally:
        session.close()

def recalculate_all_scores_no_batch():
    """
    [비배치 방식]
    전체 데이터를 한 번에 로드하여 단일 스레드로 순차 처리합니다.
    (메모리 사용량은 높으나, 배치 처리 미도입 시의 처리 시간을 측정할 수 있습니다.)
    """
    # POI 캐시 미리 초기화
    initialize_poi_cache()
    
    session = SessionLocal()
    try:
        rows = session.execute(
            text("SELECT property_id, latitude, longitude FROM property ORDER BY property_id")
        ).fetchall()
    finally:
        session.close()
    
    total_processed = 0
    processing_session = SessionLocal()  # 처리용 세션 하나만 생성
    
    try:
        for row in rows:
            if process_property(row, session=processing_session):
                total_processed += 1
            if total_processed % 100 == 0:  # 커밋 주기 조정
                processing_session.commit()
            if total_processed % 1000 == 0:
                print(f"No-batch: Processed {total_processed} properties...")
        processing_session.commit()
    except Exception as e:
        processing_session.rollback()
        print(f"Error in non-batch processing: {e}")
    finally:
        processing_session.close()
    
    return total_processed

def recalculate_all_scores_single(limit=20000, batch_size=1000):
    """
    [단일 스레드 배치 방식]
    데이터를 배치로 조회하여 단일 스레드로 처리합니다.
    테스트용으로 limit만큼 처리합니다.
    """
    # POI 캐시 미리 초기화
    initialize_poi_cache()
    
    session = SessionLocal()
    total_processed = 0
    try:
        offset = 0
        processing_session = SessionLocal()  # 처리용 세션 하나만 생성
        
        try:
            while total_processed < limit:
                query = text(
                    "SELECT property_id, latitude, longitude FROM property "
                    "ORDER BY property_id LIMIT :limit OFFSET :offset"
                )
                rows = session.execute(query, {"limit": batch_size, "offset": offset}).fetchall()
                if not rows:
                    break
                
                batch_processed = 0
                for row in rows:
                    if process_property(row, session=processing_session):
                        batch_processed += 1
                
                processing_session.commit()  # 배치마다 커밋
                total_processed += batch_processed
                offset += batch_size
                print(f"Single-threaded batch: Processed {total_processed} properties...")
            
            processing_session.commit()
        except Exception as e:
            processing_session.rollback()
            print(f"Error in single-threaded batch processing: {e}")
        finally:
            processing_session.close()
    except Exception as e:
        print(f"Error in batch query: {e}")
    finally:
        session.close()
    
    return total_processed

def recalculate_all_scores_batch(batch_size=1000, max_workers=8, limit=None):
    """
    [멀티스레드 배치 방식]
    데이터를 배치로 조회하여 각 배치를 ThreadPoolExecutor를 사용해 병렬 처리합니다.
    각 워커(스레드)는 배치 전체를 하나의 세션으로 처리합니다.
    max_workers는 풀 크기를 고려하여 조정했습니다.
    """
    # POI 캐시 미리 초기화
    initialize_poi_cache()
    
    session = SessionLocal()
    total_processed = 0
    try:
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
            
            # 각 작업자가 sub-batch를 처리하도록 함
            sub_batches = []
            sub_batch_size = len(rows) // max_workers
            if sub_batch_size < 1:
                sub_batch_size = 1
            
            for i in range(0, len(rows), sub_batch_size):
                sub_batches.append(rows[i:i + sub_batch_size])
            
            with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
                futures = [executor.submit(process_property_batch, sub_batch) for sub_batch in sub_batches]
                
                batch_processed = 0
                for future in concurrent.futures.as_completed(futures):
                    batch_processed += future.result()
            
            total_processed += batch_processed
            offset += batch_size
            print(f"Multi-threaded batch: Processed {total_processed}/{limit} properties...")
    except Exception as e:
        print(f"Error in multi-threaded batch processing: {e}")
    finally:
        session.close()
    
    return total_processed

def recalculate_incomplete_scores_batch(batch_size=1000, max_workers=8):
    """
    계산이 누락되었거나, 모든 카테고리 중 하나라도 count 값이 0인 매물에 대해,
    배치 및 멀티스레드 방식으로 점수를 재계산합니다.
    
    1. property와 property_score를 LEFT JOIN하여, property_score가 없거나,
       transport_count, restaurant_count, health_count, convenience_count,
       cafe_count, chicken_count, leisure_count 중 하나라도 0인 매물을 조회합니다.
    2. 조회된 결과를 batch_size 단위로 나누고, 각 배치를 max_workers 개의 스레드로 병렬 처리합니다.
    
    반환: 처리한 매물 수
    """
    # POI 캐시 미리 초기화
    initialize_poi_cache()
    
    session = SessionLocal()
    try:
        query = text("""
            SELECT p.property_id, p.latitude, p.longitude
            FROM property p
            LEFT JOIN property_score ps ON p.property_id = ps.property_id
            WHERE ps.property_id IS NULL
               OR ps.transport_count = 0
               OR ps.restaurant_count = 0
               OR ps.health_count = 0
               OR ps.convenience_count = 0
               OR ps.cafe_count = 0
               OR ps.chicken_count = 0
               OR ps.leisure_count = 0
            ORDER BY p.property_id
        """)
        rows = session.execute(query).fetchall()
    except Exception as e:
        print(f"Error fetching incomplete score properties: {e}")
        return 0
    finally:
        session.close()

    total = len(rows)
    print(f"Incomplete score properties found: {total}")

    total_processed = 0
    # 큰 배치를 더 작은 서브 배치로 나누어 처리
    for i in range(0, total, batch_size):
        batch_rows = rows[i:i + batch_size]
        sub_batches = []
        sub_batch_size = len(batch_rows) // max_workers
        if sub_batch_size < 1:
            sub_batch_size = 1
        
        for j in range(0, len(batch_rows), sub_batch_size):
            sub_batches.append(batch_rows[j:j + sub_batch_size])
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = [executor.submit(process_property_batch, sub_batch) for sub_batch in sub_batches]
            
            batch_processed = 0
            for future in concurrent.futures.as_completed(futures):
                batch_processed += future.result()
        
        total_processed += batch_processed
        print(f"Processed {total_processed}/{total} incomplete properties...")
    
    print(f"Recalculated scores for {total_processed} properties.")
    return total_processed