import time
import numpy as np
import logging
from sqlalchemy import text
from app.config.database import SessionLocal
from sklearn.metrics.pairwise import cosine_similarity

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
if not logger.handlers:
    handler = logging.StreamHandler()
    formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    handler.setFormatter(formatter)
    logger.addHandler(handler)

# 전역 캐시 및 캐시 타임스탬프 설정
CATEGORY_MAX_CACHE = {}
CATEGORY_MAX_CACHE_TIMESTAMP = 0
CACHE_TTL = 3600  # 1시간 (3600초)

def get_category_max_values():
    """
    property_score 테이블에서 각 카테고리별 최대값을 조회한 후, 캐시에 저장하여 반환합니다.
    TTL 이내이면 캐시된 값을 반환합니다.
    """
    global CATEGORY_MAX_CACHE, CATEGORY_MAX_CACHE_TIMESTAMP, CACHE_TTL
    current_time = time.time()
    if CATEGORY_MAX_CACHE and (current_time - CATEGORY_MAX_CACHE_TIMESTAMP < CACHE_TTL):
        logger.info("Using cached category max values.")
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
            logger.info(f"Max value for {cat}: {max_values[cat]}")
        CATEGORY_MAX_CACHE = max_values
        CATEGORY_MAX_CACHE_TIMESTAMP = current_time
        logger.info("Category max values cached.")
        return max_values
    except Exception as e:
        logger.error("Error fetching category max values: %s", e)
        return {}
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
            logger.info("No property scores found.")
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
        logger.info("Fetched %d properties.", len(properties))
        
        # DB에서 각 카테고리별 최대값 조회 (캐시 사용)
        max_values = get_category_max_values()
        logger.info("Max values used for normalization: %s", max_values)
        
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
        logger.info("User vector (normalized): %s", user_vector)

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
        if property_vectors.size == 0:
            logger.warning("No property vectors found after normalization.")
            return []
        logger.info("First property vector (normalized): %s", property_vectors[0])
        
        # 코사인 유사도 계산 (정규화된 벡터 사용)
        similarities = cosine_similarity(property_vectors, user_vector).flatten()
        logger.info("Calculated similarities for %d properties.", len(similarities))
        
        # 각 매물에 유사도 첨부
        for i, p in enumerate(properties):
            p["similarity"] = float(similarities[i])
        
        # 유사도 높은 순으로 정렬하여 상위 top_n 매물 선택
        top_properties = sorted(properties, key=lambda x: x["similarity"], reverse=True)[:top_n]
        logger.info("Top %d recommended properties: %s", top_n, [p["property_id"] for p in top_properties])
        return top_properties
    except Exception as e:
        logger.error("Error in recommend_properties: %s", e)
        return []
    finally:
        session.close()
