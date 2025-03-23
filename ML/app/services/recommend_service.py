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

# 전역 캐시: 각 카테고리별 robust(min, max) 값과 캐시 타임스탬프 설정
CATEGORY_ROBUST_CACHE = {}
CATEGORY_ROBUST_CACHE_TIMESTAMP = 0
CACHE_TTL = 3600  # 1시간

def get_category_robust_values():
    """
    property_score 테이블에서 각 카테고리별 5번째 및 95번째 백분위수를 계산하여
    robust 최소/최대값으로 캐싱 후 반환합니다.
    반환 예시:
      {
         "transport_score": (robust_min, robust_max),
         "restaurant_score": (robust_min, robust_max),
         ...
      }
    """
    global CATEGORY_ROBUST_CACHE, CATEGORY_ROBUST_CACHE_TIMESTAMP, CACHE_TTL
    current_time = time.time()
    if CATEGORY_ROBUST_CACHE and (current_time - CATEGORY_ROBUST_CACHE_TIMESTAMP < CACHE_TTL):
        logger.info("Using cached robust min-max values.")
        return CATEGORY_ROBUST_CACHE

    session = SessionLocal()
    try:
        query = text("""
            SELECT transport_score, restaurant_score, health_score,
                   convenience_score, cafe_score, chicken_score, leisure_score
            FROM property_score
        """)
        results = session.execute(query).fetchall()
        if not results:
            logger.warning("No property score data found for robust computation.")
            return {}
        data = []
        for row in results:
            data.append([
                row._mapping["transport_score"],
                row._mapping["restaurant_score"],
                row._mapping["health_score"],
                row._mapping["convenience_score"],
                row._mapping["cafe_score"],
                row._mapping["chicken_score"],
                row._mapping["leisure_score"]
            ])
        data = np.array(data, dtype=float)
        robust_min = np.percentile(data, 5, axis=0)
        robust_max = np.percentile(data, 95, axis=0)
        robust_values = {
            "transport_score": (robust_min[0], robust_max[0]),
            "restaurant_score": (robust_min[1], robust_max[1]),
            "health_score": (robust_min[2], robust_max[2]),
            "convenience_score": (robust_min[3], robust_max[3]),
            "cafe_score": (robust_min[4], robust_max[4]),
            "chicken_score": (robust_min[5], robust_max[5]),
            "leisure_score": (robust_min[6], robust_max[6])
        }
        logger.info("Computed robust min-max values: %s", robust_values)
        CATEGORY_ROBUST_CACHE = robust_values
        CATEGORY_ROBUST_CACHE_TIMESTAMP = current_time
        return robust_values
    except Exception as e:
        logger.error("Error computing robust min-max values: %s", e)
        return {}
    finally:
        session.close()

def recommend_properties(user_scores: dict, top_n=5):
    """
    사용자의 카테고리 점수와 property_score 테이블의 각 매물의 카테고리 점수 간
    코사인 유사도를 계산합니다.
    
    각 점수는 DB에서 조회한 각 카테고리별 5번째/95번째 백분위수를 이용한 robust min–max 정규화(0~1)되어 비교됩니다.
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

        # 컬럼 순서를 고정
        cols = ["property_id", "transport_score", "restaurant_score", "health_score", 
                "convenience_score", "cafe_score", "chicken_score", "leisure_score"]
        data = []
        property_ids = []
        for row in results:
            row_data = [row._mapping[col] for col in cols]
            property_ids.append(row_data[0])
            data.append(row_data[1:])  # 점수 데이터

        property_array = np.array(data, dtype=float)  # shape: (n_properties, 7)
        logger.info("Fetched %d properties.", property_array.shape[0])

        # robust min-max 값 조회
        robust_values = get_category_robust_values()
        logger.info("Robust min-max values: %s", robust_values)

        # 배열 벡터화: 각 열에 대해 robust 정규화: (x - min) / (max - min)
        robust_mins = np.array([
            robust_values["transport_score"][0],
            robust_values["restaurant_score"][0],
            robust_values["health_score"][0],
            robust_values["convenience_score"][0],
            robust_values["cafe_score"][0],
            robust_values["chicken_score"][0],
            robust_values["leisure_score"][0],
        ])
        robust_maxs = np.array([
            robust_values["transport_score"][1],
            robust_values["restaurant_score"][1],
            robust_values["health_score"][1],
            robust_values["convenience_score"][1],
            robust_values["cafe_score"][1],
            robust_values["chicken_score"][1],
            robust_values["leisure_score"][1],
        ])
        denom = robust_maxs - robust_mins
        # prevent division by zero
        denom[denom == 0] = 1
        norm_array = (property_array - robust_mins) / denom
        norm_array = np.clip(norm_array, 0, 1)
        logger.info("First property vector (robust normalized): %s", norm_array[0])

        # 사용자 벡터 정규화
        user_vals = np.array([
            user_scores.get("transport_score", 0),
            user_scores.get("restaurant_score", 0),
            user_scores.get("health_score", 0),
            user_scores.get("convenience_score", 0),
            user_scores.get("cafe_score", 0),
            user_scores.get("chicken_score", 0),
            user_scores.get("leisure_score", 0),
        ])
        user_vector = (user_vals - robust_mins) / denom
        user_vector = np.clip(user_vector, 0, 1).reshape(1, -1)
        logger.info("User vector (robust normalized): %s", user_vector)

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
