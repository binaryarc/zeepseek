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

# 캐시: z-score 계산용 (필요 시 사용)
CATEGORY_MEAN_STD_CACHE = {}
CATEGORY_MEAN_STD_CACHE_TIMESTAMP = 0
CACHE_TTL = 3600  # 1시간

def get_category_mean_std_values():
    """
    property_score 테이블에서 각 카테고리별 평균과 표준편차를 조회하여 캐싱합니다.
    (z-score 정규화를 사용할 경우에 참고)
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

def get_category_min_max_values():
    """
    property_score 테이블에서 각 카테고리별 최소/최대값을 조회합니다.
    min-max 정규화에 사용됩니다.
    """
    session = SessionLocal()
    try:
        query = text("""
            SELECT 
                MIN(transport_score) AS min_transport, MAX(transport_score) AS max_transport,
                MIN(restaurant_score) AS min_restaurant, MAX(restaurant_score) AS max_restaurant,
                MIN(health_score) AS min_health, MAX(health_score) AS max_health,
                MIN(convenience_score) AS min_convenience, MAX(convenience_score) AS max_convenience,
                MIN(cafe_score) AS min_cafe, MAX(cafe_score) AS max_cafe,
                MIN(chicken_score) AS min_chicken, MAX(chicken_score) AS max_chicken,
                MIN(leisure_score) AS min_leisure, MAX(leisure_score) AS max_leisure
            FROM property_score
        """)
        row = session.execute(query).fetchone()
        min_max = {
            "transport_score": (row._mapping["min_transport"], row._mapping["max_transport"]),
            "restaurant_score": (row._mapping["min_restaurant"], row._mapping["max_restaurant"]),
            "health_score": (row._mapping["min_health"], row._mapping["max_health"]),
            "convenience_score": (row._mapping["min_convenience"], row._mapping["max_convenience"]),
            "cafe_score": (row._mapping["min_cafe"], row._mapping["max_cafe"]),
            "chicken_score": (row._mapping["min_chicken"], row._mapping["max_chicken"]),
            "leisure_score": (row._mapping["min_leisure"], row._mapping["max_leisure"]),
        }
        return min_max
    except Exception as e:
        logger.error("Error fetching min-max values: %s", e)
        return {}
    finally:
        session.close()

def apply_mmr(similarities, property_vectors, top_n, diversity_lambda=0.5):
    """
    Maximal Marginal Relevance (MMR) 적용 함수.
    :param similarities: 사용자 벡터와 각 매물 벡터 간의 코사인 유사도 (array)
    :param property_vectors: 정규화 및 가중치가 적용된 매물 벡터 (shape: [n_properties, n_features])
    :param top_n: 최종 선택할 매물의 개수
    :param diversity_lambda: 관련성과 다양성의 균형을 위한 파라미터 (0~1)
    :return: 선택된 매물의 인덱스 리스트
    """
    selected = []
    candidate_indices = list(range(len(similarities)))
    
    # 첫번째 선택: 사용자와 가장 유사한 매물 선택
    first = np.argmax(similarities)
    selected.append(first)
    candidate_indices.remove(first)
    
    while len(selected) < top_n and candidate_indices:
        mmr_scores = []
        for i in candidate_indices:
            # 이미 선택된 매물들과의 최대 유사도 계산 (다양성 페널티)
            sim_selected = max(cosine_similarity(property_vectors[i].reshape(1, -1), property_vectors[selected]).flatten())
            score = diversity_lambda * similarities[i] - (1 - diversity_lambda) * sim_selected
            mmr_scores.append((i, score))
        best_candidate, _ = max(mmr_scores, key=lambda x: x[1])
        selected.append(best_candidate)
        candidate_indices.remove(best_candidate)
    
    return selected

def recommend_properties(user_scores: dict, top_n=5, apply_mmr_flag=True, diversity_lambda=0.5, normalization_method='minmax'):
    """
    사용자 점수와 property_score 테이블의 점수 간 코사인 유사도를 계산하여 추천합니다.
    추가로 MMR 후처리 기법을 적용해 추천 결과의 다양성을 높입니다.
    
    :param user_scores: 사용자 점수 (예: {"transport_score":0.5, ...})
    :param top_n: 추천할 매물의 개수
    :param apply_mmr_flag: MMR 후처리 적용 여부
    :param diversity_lambda: MMR 관련성 vs 다양성 균형 파라미터 (0~1)
    :param normalization_method: 'minmax' 또는 'zscore' (기본은 min-max)
    :return: 추천된 매물 리스트
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

        # 데이터 수집 및 배열 생성
        cols = ["property_id", "transport_score", "restaurant_score", "health_score", 
                "convenience_score", "cafe_score", "chicken_score", "leisure_score"]
        data = []
        property_ids = []
        for row in results:
            row_data = [row._mapping[col] for col in cols]
            property_ids.append(row_data[0])
            data.append(row_data[1:])
        property_array = np.array(data, dtype=float)
        logger.info("Fetched %d properties.", property_array.shape[0])
        
        # 카테고리별 가중치 (예시)
        # 순서: transport, restaurant, health, convenience, cafe, chicken, leisure
        category_weights = np.array([1.0, 1.0, 1.2, 1.0, 1.0, 0.8, 1.0])
        
        if normalization_method == 'minmax':
            min_max_values = get_category_min_max_values()
            mins = np.array([
                min_max_values["transport_score"][0],
                min_max_values["restaurant_score"][0],
                min_max_values["health_score"][0],
                min_max_values["convenience_score"][0],
                min_max_values["cafe_score"][0],
                min_max_values["chicken_score"][0],
                min_max_values["leisure_score"][0],
            ])
            maxs = np.array([
                min_max_values["transport_score"][1],
                min_max_values["restaurant_score"][1],
                min_max_values["health_score"][1],
                min_max_values["convenience_score"][1],
                min_max_values["cafe_score"][1],
                min_max_values["chicken_score"][1],
                min_max_values["leisure_score"][1],
            ])
            denom = maxs - mins
            denom[denom == 0] = 1  # 분모 0 방지
            norm_array = (property_array - mins) / denom
            # 사용자 점수는 이미 0~1 범위로 가정
            user_vals = np.array([
                user_scores.get("transport_score", 0),
                user_scores.get("restaurant_score", 0),
                user_scores.get("health_score", 0),
                user_scores.get("convenience_score", 0),
                user_scores.get("cafe_score", 0),
                user_scores.get("chicken_score", 0),
                user_scores.get("leisure_score", 0),
            ])
            user_vector = user_vals.reshape(1, -1)
            logger.info("Using min-max normalization.")
        else:
            # z-score 정규화 (기존 방식)
            mean_std_values = get_category_mean_std_values()
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
            stds[stds == 0] = 1
            norm_array = (property_array - means) / stds
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
            logger.info("Using z-score normalization.")
        
        # 카테고리별 가중치 적용
        norm_array = norm_array * category_weights
        user_vector = user_vector * category_weights
        
        logger.info("First property vector (normalized & weighted): %s", norm_array[0])
        logger.info("User vector (normalized & weighted): %s", user_vector)
        
        # 사용자와 각 매물 간 코사인 유사도 계산
        similarities = cosine_similarity(norm_array, user_vector).flatten()
        logger.info("Calculated similarities for %d properties.", len(similarities))
        
        if apply_mmr_flag:
            # MMR 후처리 적용하여 다양성을 고려한 최종 추천 순위 도출
            selected_indices = apply_mmr(similarities, norm_array, top_n, diversity_lambda)
            top_properties = [{"propertyId": property_ids[i], "similarity": float(similarities[i])} for i in selected_indices]
            logger.info("Top %d recommended properties after MMR: %s", top_n, [property_ids[i] for i in selected_indices])
        else:
            # 기본적으로 유사도 순으로 정렬
            properties_rec = [{"propertyId": property_ids[i], "similarity": float(similarities[i])} for i in range(len(similarities))]
            top_properties = sorted(properties_rec, key=lambda x: x["similarity"], reverse=True)[:top_n]
            logger.info("Top %d recommended properties: %s", top_n, [p["propertyId"] for p in top_properties])
            
        return top_properties

    except Exception as e:
        logger.error("Error in recommend_properties: %s", e)
        return []
    finally:
        session.close()
