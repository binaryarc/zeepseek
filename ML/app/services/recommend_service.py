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

# 전역 캐시: 각 카테고리별 최소, 최대값과 캐시 타임스탬프 설정
CATEGORY_MIN_MAX_CACHE = {}
CATEGORY_MIN_MAX_CACHE_TIMESTAMP = 0
CACHE_TTL = 3600  # 1시간

def get_category_min_max_values():
    """
    property_score 테이블에서 각 카테고리별 최소, 최대값을 조회하고 캐싱합니다.
    """
    global CATEGORY_MIN_MAX_CACHE, CATEGORY_MIN_MAX_CACHE_TIMESTAMP, CACHE_TTL
    current_time = time.time()
    if CATEGORY_MIN_MAX_CACHE and (current_time - CATEGORY_MIN_MAX_CACHE_TIMESTAMP < CACHE_TTL):
        logger.info("Using cached category min-max values.")
        return CATEGORY_MIN_MAX_CACHE

    session = SessionLocal()
    min_max_values = {}
    try:
        # 한 번의 쿼리로 모든 카테고리의 최소, 최대값을 조회하는 방법:
        query = text("""
            SELECT 
                MAX(transport_score) AS max_transport,
                MIN(transport_score) AS min_transport,
                MAX(restaurant_score) AS max_restaurant,
                MIN(restaurant_score) AS min_restaurant,
                MAX(health_score) AS max_health,
                MIN(health_score) AS min_health,
                MAX(convenience_score) AS max_convenience,
                MIN(convenience_score) AS min_convenience,
                MAX(cafe_score) AS max_cafe,
                MIN(cafe_score) AS min_cafe,
                MAX(chicken_score) AS max_chicken,
                MIN(chicken_score) AS min_chicken,
                MAX(leisure_score) AS max_leisure,
                MIN(leisure_score) AS min_leisure
            FROM property_score
        """)
        row = session.execute(query).fetchone()
        # 각 카테고리의 min, max 값 저장 (0~1 범위로 정규화 시, 분모가 0이 되지 않도록 기본값 1 사용)
        min_max_values["transport_score"] = (row._mapping["min_transport"] or 0, row._mapping["max_transport"] or 1)
        min_max_values["restaurant_score"] = (row._mapping["min_restaurant"] or 0, row._mapping["max_restaurant"] or 1)
        min_max_values["health_score"] = (row._mapping["min_health"] or 0, row._mapping["max_health"] or 1)
        min_max_values["convenience_score"] = (row._mapping["min_convenience"] or 0, row._mapping["max_convenience"] or 1)
        min_max_values["cafe_score"] = (row._mapping["min_cafe"] or 0, row._mapping["max_cafe"] or 1)
        min_max_values["chicken_score"] = (row._mapping["min_chicken"] or 0, row._mapping["max_chicken"] or 1)
        min_max_values["leisure_score"] = (row._mapping["min_leisure"] or 0, row._mapping["max_leisure"] or 1)

        logger.info("Fetched min-max values: %s", min_max_values)
        CATEGORY_MIN_MAX_CACHE = min_max_values
        CATEGORY_MIN_MAX_CACHE_TIMESTAMP = current_time
        return min_max_values
    except Exception as e:
        logger.error("Error fetching min-max values: %s", e)
        return {}
    finally:
        session.close()

def recommend_properties(user_scores: dict, top_n=5):
    """
    사용자의 카테고리 점수와 property_score 테이블의 각 매물의 카테고리 점수 간 
    코사인 유사도를 계산합니다.
    
    점수들은 DB에서 조회한 각 카테고리별 최소/최대값으로 min–max 정규화(0~1)되어 비교됩니다.
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

        # 컬럼 이름 순서 고정
        cols = ["property_id", "transport_score", "restaurant_score", "health_score", 
                "convenience_score", "cafe_score", "chicken_score", "leisure_score"]
        data = []
        property_ids = []
        for row in results:
            row_data = [row._mapping[col] for col in cols]
            property_ids.append(row_data[0])
            data.append(row_data[1:])  # 점수 데이터

        # NumPy 배열로 변환
        property_array = np.array(data, dtype=float)  # shape: (n_properties, 7)

        logger.info("Fetched %d properties.", property_array.shape[0])

        # 최소/최대값 조회 (캐시 활용)
        min_max_values = get_category_min_max_values()
        logger.info("Min-max values: %s", min_max_values)

        # 정규화: 각 컬럼별로 min-max 정규화
        # 각 열에 대해: (x - min) / (max - min)
        norm_array = np.empty_like(property_array)
        for i, cat in enumerate(["transport_score", "restaurant_score", "health_score", 
                                 "convenience_score", "cafe_score", "chicken_score", "leisure_score"]):
            cat_min, cat_max = min_max_values[cat]
            # 분모가 0이 되지 않도록 기본값 1 사용
            denom = cat_max - cat_min if (cat_max - cat_min) != 0 else 1
            norm_array[:, i] = (property_array[:, i] - cat_min) / denom

        # 사용자 점수 정규화 (min-max normalization)
        user_vector = np.array([
            (user_scores.get("transport_score", 0) - min_max_values["transport_score"][0]) / (min_max_values["transport_score"][1] - min_max_values["transport_score"][0] or 1),
            (user_scores.get("restaurant_score", 0) - min_max_values["restaurant_score"][0]) / (min_max_values["restaurant_score"][1] - min_max_values["restaurant_score"][0] or 1),
            (user_scores.get("health_score", 0) - min_max_values["health_score"][0]) / (min_max_values["health_score"][1] - min_max_values["health_score"][0] or 1),
            (user_scores.get("convenience_score", 0) - min_max_values["convenience_score"][0]) / (min_max_values["convenience_score"][1] - min_max_values["convenience_score"][0] or 1),
            (user_scores.get("cafe_score", 0) - min_max_values["cafe_score"][0]) / (min_max_values["cafe_score"][1] - min_max_values["cafe_score"][0] or 1),
            (user_scores.get("chicken_score", 0) - min_max_values["chicken_score"][0]) / (min_max_values["chicken_score"][1] - min_max_values["chicken_score"][0] or 1),
            (user_scores.get("leisure_score", 0) - min_max_values["leisure_score"][0]) / (min_max_values["leisure_score"][1] - min_max_values["leisure_score"][0] or 1),
        ]).reshape(1, -1)
        logger.info("User vector (normalized): %s", user_vector)

        # 코사인 유사도 계산
        similarities = cosine_similarity(norm_array, user_vector).flatten()
        logger.info("Calculated similarities for %d properties.", len(similarities))

        # 결과를 property_id와 유사도로 매핑
        properties = []
        for i, pid in enumerate(property_ids):
            properties.append({
                "property_id": pid,
                "similarity": float(similarities[i])
            })

        # 유사도 높은 순으로 상위 top_n 선택
        top_properties = sorted(properties, key=lambda x: x["similarity"], reverse=True)[:top_n]
        logger.info("Top %d recommended properties: %s", top_n, [p["property_id"] for p in top_properties])
        return top_properties

    except Exception as e:
        logger.error("Error in recommend_properties: %s", e)
        return []
    finally:
        session.close()
