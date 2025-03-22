import numpy as np
from sqlalchemy import text
from app.config.database import SessionLocal
from sklearn.metrics.pairwise import cosine_similarity

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
