import math
from sqlalchemy import text
from app.config.database import SessionLocal
from app.utils.haversine import haversine

def distance_score(distance):
    """
    두 지점 사이의 거리를 기반으로 점수를 계산합니다.
    가까울수록 점수가 높아지며, 공식은 1 / (1 + distance) 입니다.
    (예: 거리가 0이면 1, 멀어질수록 점수가 0에 가까워짐)
    """
    return 1 / (1 + distance)

def get_poi_by_category(category: str):
    """
    카테고리별 POI 데이터를 조회합니다.
    실제 환경에서는 DB나 API에서 데이터를 가져오지만, 여기서는 예시 데이터를 사용합니다.
    
    예시 데이터는:
      - transport, restaurant, health, convenience, cafe, chicken, leisure
      각 카테고리마다 하나의 POI 정보(위도, 경도)를 제공합니다.
    """
    sample_data = {
        "transport": [{"latitude": 37.55, "longitude": 126.97}],
        "restaurant": [{"latitude": 37.551, "longitude": 126.971}],
        "health": [{"latitude": 37.552, "longitude": 126.972}],
        "convenience": [{"latitude": 37.553, "longitude": 126.973}],
        "cafe": [{"latitude": 37.554, "longitude": 126.974}],
        "chicken": [{"latitude": 37.555, "longitude": 126.975}],
        "leisure": [{"latitude": 37.556, "longitude": 126.976}],
    }
    return sample_data.get(category, [])

def calculate_category_score(property_lat, property_lon, poi_list, radius=1.0, alpha=0.5, beta=0.5):
    """
    주어진 POI 리스트에 대해 카테고리 점수를 계산합니다.
    
    - radius: 매물과 POI 사이의 최대 고려 반경(1km)
    - alpha: 반경 내 POI 개수(개수 점수)에 적용할 가중치
    - beta: 가장 가까운 POI와의 거리(거리 기반 점수)에 적용할 가중치
    
    처리 과정:
      1. 모든 POI에 대해 haversine 공식을 사용하여 매물과의 거리를 계산.
      2. 거리가 radius 이하인 경우, POI 개수(count)를 증가시키고, 가장 짧은 거리를 기록.
      3. 만약 반경 내 POI가 없으면, min_distance를 radius 값으로 설정합니다.
      4. 개수 점수는 단순히 count 값입니다.
      5. 거리 기반 점수는 distance_score(min_distance)를 사용합니다.
      6. 최종 카테고리 점수는: alpha * (개수 점수) + beta * (거리 기반 점수)
    
    반환값: (개수, 거리 기반 점수, 최종 카테고리 점수)
    """
    count = 0
    min_distance = float('inf')
    for poi in poi_list:
        d = haversine(property_lat, property_lon, poi["latitude"], poi["longitude"])
        # 1km 반경 내에 있는 POI만 고려합니다.
        if d <= radius:
            count += 1
            # 반경 내 POI 중 가장 가까운 거리를 기록합니다.
            if d < min_distance:
                min_distance = d
    if count == 0:
        # 반경 내 POI가 없으면, 최소 거리를 radius로 설정하여 낮은 점수를 부여합니다.
        min_distance = radius
    count_score = count              # 개수 점수는 단순 count 값
    dist_score = distance_score(min_distance)  # 거리 기반 점수 계산
    # 최종 카테고리 점수 = (alpha * 개수 점수) + (beta * 거리 기반 점수)
    category_score = alpha * count_score + beta * dist_score
    return count, dist_score, category_score

def compute_property_score(property_data: dict):
    """
    매물 데이터(property_data는 'latitude'와 'longitude' 포함)를 받아,
    각 POI 카테고리(transport, restaurant, health, convenience, cafe, chicken, leisure)에 대한 점수를 계산합니다.
    
    각 카테고리별로 미리 설정한 alpha, beta 값을 사용하여 개수 점수와 거리 점수를 조합한 후,
    모든 카테고리 점수를 합산해 전체 매물 점수를 산출합니다.
    
    반환값은 각 카테고리의 개수, 거리 점수, 카테고리 점수 및 전체 점수를 포함하는 dict입니다.
    """
    lat = property_data["latitude"]
    lon = property_data["longitude"]
    
    # 각 카테고리별로 경험적으로 설정한 가중치 (alpha, beta)
    categories = {
        "transport": {"alpha": 0.5, "beta": 0.5},
        "restaurant": {"alpha": 0.5, "beta": 0.5},
        "health": {"alpha": 0.6, "beta": 0.4},
        "convenience": {"alpha": 0.5, "beta": 0.5},
        "cafe": {"alpha": 0.4, "beta": 0.6},
        "chicken": {"alpha": 0.4, "beta": 0.6},
        "leisure": {"alpha": 0.5, "beta": 0.5}
    }
    
    overall_score = 0
    results = {}
    
    for cat, weight_params in categories.items():
        poi_list = get_poi_by_category(cat)
        count, dist_score, cat_score = calculate_category_score(
            lat, lon, poi_list,
            radius=1.0,
            alpha=weight_params["alpha"],
            beta=weight_params["beta"]
        )
        results[f"{cat}_count"] = count
        results[f"{cat}_distance_score"] = dist_score
        results[f"{cat}_score"] = cat_score
        overall_score += cat_score  # 모든 카테고리 점수의 단순 합산
    results["overall_score"] = overall_score
    return results

def update_property_score(property_id: int, score_data: dict):
    """
    매물 ID에 해당하는 property_score 테이블의 점수를 업데이트합니다.
    만약 해당 매물에 대한 기록이 없으면 새로 삽입합니다.
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
            "leisure_count = :leisure_count, leisure_score = :leisure_score, "
            "overall_score = :overall_score "
            "WHERE property_id = :property_id"
        )
        params = {"property_id": property_id, **score_data}
        result = session.execute(stmt, params)
        if result.rowcount == 0:
            insert_stmt = text(
                "INSERT INTO property_score (property_id, transport_count, transport_score, restaurant_count, restaurant_score, "
                "health_count, health_score, convenience_count, convenience_score, cafe_count, cafe_score, "
                "chicken_count, chicken_score, leisure_count, leisure_score, overall_score) "
                "VALUES (:property_id, :transport_count, :transport_score, :restaurant_count, :restaurant_score, "
                ":health_count, :health_score, :convenience_count, :convenience_score, :cafe_count, :cafe_score, "
                ":chicken_count, :chicken_score, :leisure_count, :leisure_score, :overall_score)"
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

def recalculate_all_scores():
    """
    property 테이블의 모든 매물에 대해 점수를 재계산하고, property_score 테이블을 업데이트합니다.
    """
    session = SessionLocal()
    try:
        properties = session.execute(text("SELECT property_id, latitude, longitude FROM property")).fetchall()
        count = 0
        for row in properties:
            prop_id = row._mapping["property_id"]
            lat = row._mapping["latitude"]
            lon = row._mapping["longitude"]
            score_data = compute_property_score({"latitude": lat, "longitude": lon})
            update_property_score(prop_id, score_data)
            count += 1
        return count
    finally:
        session.close()
