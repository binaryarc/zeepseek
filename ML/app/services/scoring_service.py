import math
from sqlalchemy import text
from app.config.database import SessionLocal
from app.utils.haversine import haversine

def distance_score(distance):
    """
    가까울수록 높은 점수를 부여: 1 / (1 + distance)
    """
    return 1 / (1 + distance)

def get_poi_by_category(category: str):
    """
    카테고리별 POI 데이터 조회 (예시 데이터; 실제 환경에서는 DB 또는 API에서 조회)
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
    주어진 POI 리스트에 대해:
    - 반경 내 POI 개수를 count (개수 점수)
    - 가장 가까운 POI와의 거리를 바탕으로 거리 점수를 계산
    두 점수를 α와 β 가중치로 결합하여 카테고리 점수를 산출합니다.
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
        min_distance = radius  # 반경 내 POI가 없으면, 최종 점수에 낮은 영향
    count_score = count            # 개수 점수: 단순 count 사용
    dist_score = distance_score(min_distance)
    # 최종 카테고리 점수: α * 개수 점수 + β * (거리 기반 점수)
    category_score = alpha * count_score + beta * dist_score
    return count, dist_score, category_score

def compute_property_score(property_data: dict):
    """
    매물 데이터(property_data: dict에 'latitude'와 'longitude' 포함)를 받아,
    각 카테고리(transport, restaurant, health, convenience, cafe, chicken, leisure)의 점수를 계산하고,
    전체 점수를 합산하여 반환합니다.
    """
    lat = property_data["latitude"]
    lon = property_data["longitude"]
    
    # 각 카테고리별 α와 β 값 (경험적으로 설정)
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
        count, dist_score, cat_score = calculate_category_score(lat, lon, poi_list,
                                                                radius=1.0,
                                                                alpha=weight_params["alpha"],
                                                                beta=weight_params["beta"])
        results[f"{cat}_count"] = count
        results[f"{cat}_distance_score"] = dist_score
        results[f"{cat}_score"] = cat_score
        overall_score += cat_score
    results["overall_score"] = overall_score
    return results

def update_property_score(property_id: int, score_data: dict):
    """
    매물 ID에 해당하는 PROPERTY_SCORE 테이블의 점수를 업데이트합니다.
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
    PROPERTY 테이블의 모든 매물에 대해 점수를 재계산하고 PROPERTY_SCORE 테이블을 업데이트합니다.
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
