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

# 전역 캐시: 각 카테고리별 평균, 표준편차와 캐시 타임스탬프 설정
CATEGORY_MEAN_STD_CACHE = {}
CATEGORY_MEAN_STD_CACHE_TIMESTAMP = 0
CACHE_TTL = 3600  # 1시간

def get_category_mean_std_values():
    """
    property_score 테이블에서 각 카테고리별 평균과 표준편차를 조회하여 캐싱합니다.
    반환 예시:
      {
         "transport_score": (avg, std),
         "restaurant_score": (avg, std),
         ...
      }
    """
    global CATEGORY_MEAN_STD_CACHE, CATEGORY_MEAN_STD_CACHE_TIMESTAMP, CACHE_TTL
    current_time = time.time()
    if CATEGORY_MEAN_STD_CACHE and (current_time - CATEGORY_MEAN_STD_CACHE_TIMESTAMP < CACHE_TTL):
        logger.info("Using cached category mean-std values.")
        return CATEGORY_MEAN_STD_CACHE

    session = SessionLocal()
    try:
        query = text("""
            SELECT 
                AVG(transport_score) AS avg_transport,
                STDDEV(transport_score) AS std_transport,
                AVG(restaurant_score) AS avg_restaurant,
                STDDEV(restaurant_score) AS std_restaurant,
                AVG(health_score) AS avg_health,
                STDDEV(health_score) AS std_health,
                AVG(convenience_score) AS avg_convenience,
                STDDEV(convenience_score) AS std_convenience,
                AVG(cafe_score) AS avg_cafe,
                STDDEV(cafe_score) AS std_cafe,
                AVG(chicken_score) AS avg_chicken,
                STDDEV(chicken_score) AS std_chicken,
                AVG(leisure_score) AS avg_leisure,
                STDDEV(leisure_score) AS std_leisure
            FROM property_score
        """)
        row = session.execute(query).fetchone()
        # 만약 표준편차가 0이면 1로 대체하여 분모가 0이 되지 않도록 처리
        mean_std = {
            "transport_score": (row._mapping["avg_transport"], row._mapping["std_transport"] if row._mapping["std_transport"] else 1),
            "restaurant_score": (row._mapping["avg_restaurant"], row._mapping["std_restaurant"] if row._mapping["std_restaurant"] else 1),
            "health_score": (row._mapping["avg_health"], row._mapping["std_health"] if row._mapping["std_health"] else 1),
            "convenience_score": (row._mapping["avg_convenience"], row._mapping["std_convenience"] if row._mapping["std_convenience"] else 1),
            "cafe_score": (row._mapping["avg_cafe"], row._mapping["std_cafe"] if row._mapping["std_cafe"] else 1),
            "chicken_score": (row._mapping["avg_chicken"], row._mapping["std_chicken"] if row._mapping["std_chicken"] else 1),
            "leisure_score": (row._mapping["avg_leisure"], row._mapping["std_leisure"] if row._mapping["std_leisure"] else 1)
        }
        logger.info("Fetched mean-std values: %s", mean_std)
        CATEGORY_MEAN_STD_CACHE = mean_std
        CATEGORY_MEAN_STD_CACHE_TIMESTAMP = current_time
        return mean_std
    except Exception as e:
        logger.error("Error fetching mean-std values: %s", e)
        return {}
    finally:
        session.close()

def recommend_properties(user_scores: dict, top_n=5):
    """
    사용자의 카테고리 점수와 property_score 테이블의 각 매물의 카테고리 점수 간 
    코사인 유사도를 계산합니다.
    
    각 점수는 DB에서 조회한 각 카테고리별 평균, 표준편차를 이용한 z-score 정규화로 변환됩니다.
    벡터 구성 순서는:
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

        # 컬럼 순서 고정
        cols = ["property_id", "transport_score", "restaurant_score", "health_score", 
                "convenience_score", "cafe_score", "chicken_score", "leisure_score"]
        data = []
        property_ids = []
        for row in results:
            row_data = [row._mapping[col] for col in cols]
            property_ids.append(row_data[0])
            data.append(row_data[1:])
        property_array = np.array(data, dtype=float)  # shape: (n_properties, 7)
        logger.info("Fetched %d properties.", property_array.shape[0])

        # 평균 및 표준편차 조회 (캐시 활용)
        mean_std_values = get_category_mean_std_values()
        logger.info("Mean-std values: %s", mean_std_values)

        # 각 컬럼별로 z-score 정규화: (x - mean) / std
        means = np.array([
            mean_std_values["transport_score"][0],
            mean_std_values["restaurant_score"][0],
            mean_std_values["health_score"][0],
            mean_std_values["convenience_score"][0],
            mean_std_values["cafe_score"][0],
            mean_std_values["chicken_score"][0],
            mean_std_values["leisure_score"][0],
        ])
        stds = np.array([
            mean_std_values["transport_score"][1],
            mean_std_values["restaurant_score"][1],
            mean_std_values["health_score"][1],
            mean_std_values["convenience_score"][1],
            mean_std_values["cafe_score"][1],
            mean_std_values["chicken_score"][1],
            mean_std_values["leisure_score"][1],
        ])
        # 방어: std가 0이면 1로 대체
        stds[stds == 0] = 1
        norm_array = (property_array - means) / stds
        # (원하는 경우 정규화된 값을 0~1 범위로 선형 변환할 수 있으나, 코사인 유사도는 방향만 비교함)

        logger.info("First property vector (z-score normalized): %s", norm_array[0])

        # 사용자 벡터 z-score 정규화
        user_vals = np.array([
            user_scores.get("transport_score", 0),
            user_scores.get("restaurant_score", 0),
            user_scores.get("health_score", 0),
            user_scores.get("convenience_score", 0),
            user_scores.get("cafe_score", 0),
            user_scores.get("chicken_score", 0),
            user_scores.get("leisure_score", 0),
        ])
        user_vector = ((user_vals - means) / stds).reshape(1, -1)
        logger.info("User vector (z-score normalized): %s", user_vector)

        # 코사인 유사도 계산
        similarities = cosine_similarity(norm_array, user_vector).flatten()
        logger.info("Calculated similarities for %d properties.", len(similarities))

        # 결과 매핑
        properties_rec = []
        for i, pid in enumerate(property_ids):
            properties_rec.append({
                "property_id": pid,
                "similarity": float(similarities[i])
            })
        top_properties = sorted(properties_rec, key=lambda x: x["similarity"], reverse=True)[:top_n]
        logger.info("Top %d recommended properties: %s", top_n, [p["property_id"] for p in top_properties])
        return top_properties

    except Exception as e:
        logger.error("Error in recommend_properties: %s", e)
        return []
    finally:
        session.close()
