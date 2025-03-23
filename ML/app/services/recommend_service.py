import time
import numpy as np
from sqlalchemy import text
from app.config.database import SessionLocal
from sklearn.metrics.pairwise import cosine_similarity

# 전역 캐시 및 캐시 타임스탬프 설정
CATEGORY_MAX_CACHE = {}
CATEGORY_MAX_CACHE_TIMESTAMP = 0
CACHE_TTL = 3600  # 1시간 (3600초)

def get_category_max_values():
    """
    property_score 테이블에서 각 카테고리별 최대값을 조회한 후, 캐시에 저장하여 반환합니다.
    만약 캐시가 존재하고 TTL(3600초) 이내이면 캐시된 값을 반환합니다.
    반환 예시:
      {
         "transport_score": ...,
         "restaurant_score": ...,
         "health_score": ...,
         "convenience_score": ...,
         "cafe_score": ...,
         "chicken_score": ...,
         "leisure_score": ...
      }
    """
    global CATEGORY_MAX_CACHE, CATEGORY_MAX_CACHE_TIMESTAMP, CACHE_TTL
    current_time = time.time()
    if CATEGORY_MAX_CACHE and (current_time - CATEGORY_MAX_CACHE_TIMESTAMP < CACHE_TTL):
        return CATEGORY_MAX_CACHE

    session = SessionLocal()
    max_values = {}
    try:
        categories = [
            "transport_score", "restaurant_score", "health_score", 
            "convenience_score", "cafe_score", "chicken_score", "leisure_score"
        ]
        for cat in categories:
            query = text(f"SELECT MAX({cat}) as max_val FROM property_score")
            result = session.execute(query).scalar()
            max_values[cat] = result if result and result > 0 else 1
        # 캐시 및 타임스탬프 갱신
        CATEGORY_MAX_CACHE = max_values
        CATEGORY_MAX_CACHE_TIMESTAMP = current_time
        return max_values
    finally:
        session.close()

def recommend_properties(user_scores: dict, top_n=5):
    """
    사용자의 카테고리 점수와 property_score 테이블의 각 매물의 카테고리 점수 간 
    코사인 유사도를 계산합니다.
    
    점수들은 DB에서 조회한 각 카테고리별 최대값으로 정규화(0~1)되어 비교됩니다.
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
        
        # DB에서 각 카테고리별 최대값을 캐시에서 조회 (없으면 DB에서 가져와 캐싱됨)
        max_values = get_category_max_values()
        
        # 사용자 점수 벡터 정규화 (0~1 범위)
        user_vector = np.array([
            user_scores.get("transport_score", 0) / max_values["transport_score"],
            user_scores.get("restaurant_score", 0) / max_values["restaurant_score"],
            user_scores.get("health_score", 0) / max_values["health_score"],
            user_scores.get("convenience_score", 0) / max_values["convenience_score"],
            user_scores.get("cafe_score", 0) / max_values["cafe_score"],
            user_scores.get("chicken_score", 0) / max_values["chicken_score"],
            user_scores.get("leisure_score", 0) / max_values["leisure_score"],
        ]).reshape(1, -1)
        
        # 각 매물의 점수 벡터 정규화
        property_vectors = np.array([
            [
                p["transport_score"] / max_values["transport_score"],
                p["restaurant_score"] / max_values["restaurant_score"],
                p["health_score"] / max_values["health_score"],
                p["convenience_score"] / max_values["convenience_score"],
                p["cafe_score"] / max_values["cafe_score"],
                p["chicken_score"] / max_values["chicken_score"],
                p["leisure_score"] / max_values["leisure_score"],
            ]
            for p in properties
        ])
        
        # 코사인 유사도 계산 (정규화된 벡터 사용)
        similarities = cosine_similarity(property_vectors, user_vector).flatten()
        
        # 각 매물에 유사도 첨부
        for i, p in enumerate(properties):
            p["similarity"] = float(similarities[i])
        
        # 유사도 높은 순으로 정렬하여 상위 top_n 매물 선택
        top_properties = sorted(properties, key=lambda x: x["similarity"], reverse=True)[:top_n]
        return top_properties
    finally:
        session.close()
