import math
import numpy as np
from sqlalchemy import text
from app.config.database import SessionLocal
from app.utils.haversine import haversine
from sklearn.metrics.pairwise import cosine_similarity

def distance_score(distance):
    """
    두 지점 사이의 거리를 기반으로 점수를 계산합니다.
    가까울수록 점수가 높아지며, 공식은 1 / (1 + distance) 입니다.
    (예: 거리가 0이면 1, 멀어질수록 0에 가까워짐)
    """
    return 1 / (1 + distance)

def get_poi_by_category(category: str):
    """
    카테고리별 POI 데이터를 조회합니다.
    실제 환경에서는 DB나 API에서 데이터를 가져오지만, 여기서는 예시 데이터를 사용합니다.
    
    예시 데이터는 각 카테고리마다 하나 이상의 POI 정보를 포함합니다.
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
    
    - radius: 매물과 POI 사이의 최대 고려 반경 (1km)
    - alpha: 반경 내 POI 개수(개수 점수)에 적용할 가중치
    - beta: 가장 가까운 POI와의 거리(거리 기반 점수)에 적용할 가중치
    
    처리 과정:
      1. 모든 POI에 대해 haversine 공식을 사용하여 매물과의 거리를 계산.
      2. 거리가 radius 이하인 경우, POI 개수를 증가시키고, 가장 짧은 거리를 기록.
      3. 반경 내 POI가 없으면 min_distance를 radius 값으로 설정합니다.
      4. 개수 점수는 단순 count 값이며, 거리 기반 점수는 distance_score(min_distance)를 사용합니다.
      5. 최종 카테고리 점수는: alpha * (개수 점수) + beta * (거리 기반 점수)
    
    반환값: (개수, 거리 기반 점수, 최종 카테고리 점수)
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
    count_score = count
    dist_score = distance_score(min_distance)
    category_score = alpha * count_score + beta * dist_score
    return count, dist_score, category_score

def compute_property_score(property_data: dict):
    """
    매물 데이터(property_data에 'latitude'와 'longitude'가 포함됨)를 받아,
    각 POI 카테고리(transport, restaurant, health, convenience, cafe, chicken, leisure)에 대한 점수를 계산합니다.
    
    반환값은 각 카테고리의 개수와 점수를 포함하는 dict입니다.
      - transport_count, transport_score
      - restaurant_count, restaurant_score
      - health_count, health_score
      - convenience_count, convenience_score
      - cafe_count, cafe_score
      - chicken_count, chicken_score
      - leisure_count, leisure_score
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
    매물 ID에 해당하는 property_score 테이블의 점수를 업데이트합니다.
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

def recalculate_all_scores():
    """
    property 테이블의 모든 매물에 대해 점수를 재계산하여 property_score 테이블을 업데이트합니다.
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

def recommend_properties(user_scores: dict, top_n=5):
    """
    사용자의 카테고리 점수와 property_score 테이블의 각 매물의 카테고리 점수 간 코사인 유사도를 계산하여,
    상위 top_n 매물 리스트를 반환합니다.
    
    양쪽 벡터 구성 순서는:
      [transport_score, restaurant_score, health_score, convenience_score, cafe_score, chicken_score, leisure_score]
    """
    session = SessionLocal()
    try:
        query = text(
            "SELECT property_id, transport_score, restaurant_score, health_score, "
            "convenience_score, cafe_score, chicken_score, leisure_score "
            "FROM property_score"
        )
        results = session.execute(query).fetchall()
        if not results:
            return []
        
        properties = []
        for row in results:
            properties.append({
                "property_id": row._mapping["property_id"],
                "transport_score": row._mapping["transport_score"],
                "restaurant_score": row._mapping["restaurant_score"],
                "health_score": row._mapping["health_score"],
                "convenience_score": row._mapping["convenience_score"],
                "cafe_score": row._mapping["cafe_score"],
                "chicken_score": row._mapping["chicken_score"],
                "leisure_score": row._mapping["leisure_score"],
            })
        
        # 사용자 점수 벡터 구성
        user_vector = np.array([
            user_scores.get("transport_score", 0),
            user_scores.get("restaurant_score", 0),
            user_scores.get("health_score", 0),
            user_scores.get("convenience_score", 0),
            user_scores.get("cafe_score", 0),
            user_scores.get("chicken_score", 0),
            user_scores.get("leisure_score", 0)
        ]).reshape(1, -1)
        
        # 각 매물의 점수 벡터 구성
        property_vectors = np.array([
            [
                p["transport_score"],
                p["restaurant_score"],
                p["health_score"],
                p["convenience_score"],
                p["cafe_score"],
                p["chicken_score"],
                p["leisure_score"],
            ]
            for p in properties
        ])
        
        # 코사인 유사도 계산
        similarities = cosine_similarity(property_vectors, user_vector).flatten()
        
        # 각 매물에 유사도 첨부
        for i, p in enumerate(properties):
            p["similarity"] = float(similarities[i])
        
        # 유사도 높은 순으로 정렬하여 상위 top_n 매물 선택
        top_properties = sorted(properties, key=lambda x: x["similarity"], reverse=True)[:top_n]
        return top_properties
    finally:
        session.close()
