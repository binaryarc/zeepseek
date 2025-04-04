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

# 캐시: z-score 정규화에 필요한 값 (필요 시 사용)
CATEGORY_MEAN_STD_CACHE = {}
CATEGORY_MEAN_STD_CACHE_TIMESTAMP = 0
CACHE_TTL = 3600  # 1시간

# 글로벌 캐시: 매물 벡터와 property_id 저장 (5분 TTL)
PROPERTY_VECTORS_CACHE = None
PROPERTY_IDS_CACHE = None
PROPERTY_CACHE_TIMESTAMP = 0
PROPERTY_CACHE_TTL = 300  # 5분

# 사용자 선호도 캐시: 사용자 선호도 정보 저장 (10분 TTL)
USER_PREFERENCE_CACHE = {}
USER_PREFERENCE_CACHE_TIMESTAMP = 0
USER_PREFERENCE_CACHE_TTL = 600  # 10분


def get_category_mean_std_values():
    """
    property_score 테이블에서 각 카테고리별 평균과 표준편차를 조회하여 캐싱합니다.
    (z-score 정규화를 사용할 경우에 참고)
    """
    global CATEGORY_MEAN_STD_CACHE, CATEGORY_MEAN_STD_CACHE_TIMESTAMP, CACHE_TTL
    현재_시간 = time.time()
    if CATEGORY_MEAN_STD_CACHE and (현재_시간 - CATEGORY_MEAN_STD_CACHE_TIMESTAMP < CACHE_TTL):
        logger.info("[get_category_mean_std_values] 이미 캐싱된 카테고리 평균/표준편차 값을 사용합니다.")
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
        logger.info("[get_category_mean_std_values] 카테고리별 평균/표준편차 조회 완료: %s", mean_std)
        CATEGORY_MEAN_STD_CACHE = mean_std
        CATEGORY_MEAN_STD_CACHE_TIMESTAMP = 현재_시간
        return mean_std
    except Exception as e:
        logger.error("[get_category_mean_std_values] 평균/표준편차 조회 중 오류 발생: %s", e)
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
        logger.error("[get_category_min_max_values] min-max 조회 중 오류 발생: %s", e)
        return {}
    finally:
        session.close()


def load_property_vectors():
    """
    매물 벡터와 property_id를 DB에서 읽어 글로벌 캐시에 저장합니다.
    캐시 TTL 내라면 기존 캐시를 반환합니다.
    """
    global PROPERTY_VECTORS_CACHE, PROPERTY_IDS_CACHE, PROPERTY_CACHE_TIMESTAMP
    현재_시간 = time.time()
    if PROPERTY_VECTORS_CACHE is not None and (현재_시간 - PROPERTY_CACHE_TIMESTAMP < PROPERTY_CACHE_TTL):
        logger.info("[load_property_vectors] 캐시된 매물 벡터 정보를 재사용합니다.")
        return PROPERTY_VECTORS_CACHE, PROPERTY_IDS_CACHE

    session = SessionLocal()
    try:
        query = text(
            "SELECT property_id, transport_score, restaurant_score, health_score, "
            "convenience_score, cafe_score, chicken_score, leisure_score "
            "FROM property_score"
        )
        results = session.execute(query).fetchall()
        if not results:
            logger.warning("[load_property_vectors] DB에서 매물 점수를 찾지 못했습니다. None 반환.")
            return None, None

        property_ids = []
        data = []
        cols = ["property_id", "transport_score", "restaurant_score", "health_score",
                "convenience_score", "cafe_score", "chicken_score", "leisure_score"]
        for row in results:
            row_data = [row._mapping[col] for col in cols]
            property_ids.append(row_data[0])
            data.append(row_data[1:])
        property_array = np.array(data, dtype=float)
        PROPERTY_VECTORS_CACHE = property_array
        PROPERTY_IDS_CACHE = property_ids
        PROPERTY_CACHE_TIMESTAMP = 현재_시간
        logger.info("[load_property_vectors] %d개의 매물 벡터를 캐시에 로드했습니다.", property_array.shape[0])
        return property_array, property_ids
    except Exception as e:
        logger.error("[load_property_vectors] 매물 벡터 로드 중 오류 발생: %s", e)
        return None, None
    finally:
        session.close()


def apply_mmr(similarities, property_vectors, top_n, diversity_lambda=0.5):
    """
    MMR을 적용하여 추천 결과의 다양성을 높입니다.
    """
    selected = []
    candidate_indices = list(range(len(similarities)))
    
    # 첫 번째 선택: 사용자와 가장 유사한 매물 선택
    first = np.argmax(similarities)
    selected.append(first)
    candidate_indices.remove(first)
    
    while len(selected) < top_n and candidate_indices:
        mmr_scores = []
        for i in candidate_indices:
            sim_selected = max(cosine_similarity(property_vectors[i].reshape(1, -1),
                                                 property_vectors[selected]).flatten())
            score = diversity_lambda * similarities[i] - (1 - diversity_lambda) * sim_selected
            mmr_scores.append((i, score))
        best_candidate, _ = max(mmr_scores, key=lambda x: x[1])
        selected.append(best_candidate)
        candidate_indices.remove(best_candidate)
    
    return selected


def get_age_group(age):
    """
    나이를 연령대로 변환합니다.
    """
    try:
        age = int(age)
        if 20 <= age < 30:
            return '20s'
        elif 30 <= age < 40:
            return '30s'
        elif 40 <= age < 50:
            return '40s'
        elif age >= 50:
            return '50s_plus'
        else:
            logger.warning("[get_age_group] 나이 %d는 20 미만이므로 20대(20s)로 처리합니다.", age)
            return '20s'
    except (ValueError, TypeError):
        logger.warning("[get_age_group] 나이 값 %s가 유효하지 않아 30대(30s)로 기본 처리합니다.", age)
        return '30s'


def convert_gender_code(gender_code):
    """
    쿠키에는 성별 데이터가 0/1로 넘어오므로, 이를 문자열로 변환합니다.
    """
    if gender_code == 1:
        return 'male'
    elif gender_code == 0:
        return 'female'
    else:
        logger.warning("[convert_gender_code] 알 수 없는 성별 코드: %d → 'male'로 처리.", gender_code)
        return 'male'


def get_demographic_weight_adjustments(gender, age_group):
    """
    성별과 연령대에 따른 카테고리별 가중치 조정값을 반환합니다.
    """
    if isinstance(gender, int):
        gender = convert_gender_code(gender)
    elif not isinstance(gender, str):
        logger.warning("[get_demographic_weight_adjustments] 성별 타입이 예상과 달라 'male'로 처리: %s", type(gender))
        gender = 'male'
    
    기본값 = np.array([0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0])
    
    조정표 = {
        ('male', '20s'): np.array([0.0, +0.2, -0.2, +0.1, -0.1, +0.3, +0.4]),
        ('female', '20s'): np.array([+0.1, +0.1, -0.1, 0.0, +0.5, 0.0, +0.2]),

        ('male', '30s'): np.array([-0.2, +0.1, 0.0, 0.0, 0.0, +0.1, +0.2]),
        ('female', '30s'): np.array([-0.1, 0.0, +0.1, +0.1, +0.3, -0.1, +0.1]),

        ('male', '40s'): np.array([-0.2, 0.0, +0.2, 0.0, -0.1, 0.0, +0.1]),
        ('female', '40s'): np.array([-0.1, 0.0, +0.3, +0.1, +0.1, -0.1, 0.0]),

        ('male', '50s_plus'): np.array([-0.1, -0.1, +0.5, +0.1, -0.2, -0.2, +0.2]),
        ('female', '50s_plus'): np.array([0.0, -0.1, +0.5, +0.2, 0.0, -0.2, 0.0])
    }
    
    key = (gender, age_group)
    if key in 조정표:
        logger.info("[get_demographic_weight_adjustments] (%s, %s)에 대한 가중치 조정 적용: %s",
                    gender, age_group, 조정표[key])
        return 조정표[key]
    else:
        logger.warning("[get_demographic_weight_adjustments] 정의되지 않은 (성별, 연령대)이므로 기본값 사용: (%s, %s)", gender, age_group)
        return 기본값


def get_user_preference_weights(user_id):
    """
    user_preference 테이블에서 사용자가 중요하게 생각하는 항목(1)에 추가 가중치를 줍니다.
    """
    global USER_PREFERENCE_CACHE, USER_PREFERENCE_CACHE_TIMESTAMP, USER_PREFERENCE_CACHE_TTL
    현재_시간 = time.time()
    
    if user_id in USER_PREFERENCE_CACHE and (현재_시간 - USER_PREFERENCE_CACHE_TIMESTAMP < USER_PREFERENCE_CACHE_TTL):
        logger.info("[get_user_preference_weights] 캐시에 있는 사용자(%s)의 선호도 정보를 사용합니다.", user_id)
        return USER_PREFERENCE_CACHE[user_id]
    
    기본_선호도 = np.array([1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0])

    if not user_id:
        logger.info("[get_user_preference_weights] user_id가 없으므로 기본 선호도 사용.")
        return 기본_선호도
    
    session = SessionLocal()
    try:
        query = text("""
            SELECT 
                transport, restaurant, health, 
                convenience, cafe, leisure, safe
            FROM user_preference
            WHERE user_id = :user_id
        """)
        result = session.execute(query, {"user_id": user_id}).fetchone()
        
        if not result:
            logger.info("[get_user_preference_weights] user_id=%s 사용자의 선호도 정보를 찾지 못해 기본값 사용.", user_id)
            return 기본_선호도
        
        preference_adjustments = np.array([
            1.0 if result._mapping["transport"] == 1 else 0.0,
            1.0 if result._mapping["restaurant"] == 1 else 0.0,
            1.0 if result._mapping["health"] == 1 else 0.0,
            1.0 if result._mapping["convenience"] == 1 else 0.0,
            1.0 if result._mapping["cafe"] == 1 else 0.0,
            1.0 if result._mapping["safe"] == 1 else 0.0,
            1.0 if result._mapping["leisure"] == 1 else 0.0
        ])
        
        USER_PREFERENCE_CACHE[user_id] = preference_adjustments
        USER_PREFERENCE_CACHE_TIMESTAMP = 현재_시간
        
        logger.info("[get_user_preference_weights] user_id=%s → 선호도 가중치: %s", user_id, preference_adjustments)
        return preference_adjustments
    
    except Exception as e:
        logger.error("[get_user_preference_weights] 사용자 선호도 조회 중 오류 발생: %s", e)
        return 기본_선호도
    finally:
        session.close()


def get_category_priority(gender, age_group):
    """
    성별과 연령대에 따른 카테고리 우선순위를 반환합니다.
    """
    if isinstance(gender, int):
        gender = convert_gender_code(gender)
    elif not isinstance(gender, str):
        logger.warning("[get_category_priority] 성별 타입이 알 수 없어 'male'로 처리: %s", type(gender))
        gender = 'male'
    
    기본_우선순위 = np.array([4, 5, 3, 2, 6, 1, 7])
    
    우선순위_표 = {
        ('male', '20s'): np.array([3, 5, 1, 2, 4, 6, 7]),
        ('female', '20s'): np.array([3, 4, 1, 2, 7, 5, 6]),
        
        ('male', '30s'): np.array([2, 5, 3, 4, 6, 1, 7]),
        ('female', '30s'): np.array([2, 4, 5, 3, 7, 1, 6]),
        
        ('male', '40s'): np.array([2, 4, 7, 3, 1, 5, 6]),
        ('female', '40s'): np.array([2, 3, 7, 5, 6, 1, 4]),
        
        ('male', '50s_plus'): np.array([3, 2, 7, 5, 1, 4, 6]),
        ('female', '50s_plus'): np.array([3, 2, 7, 6, 4, 1, 5])
    }
    
    key = (gender, age_group)
    if key in 우선순위_표:
        logger.info("[get_category_priority] (%s, %s)에 대한 카테고리 우선순위: %s", gender, age_group, 우선순위_표[key])
        return 우선순위_표[key]
    else:
        logger.warning("[get_category_priority] 정의되지 않은 (성별, 연령대) → 기본 우선순위 사용: (%s, %s)", gender, age_group)
        return 기본_우선순위


def recommend_properties(user_scores: dict, top_n=5, apply_mmr_flag=True, diversity_lambda=0.5, 
                         normalization_method='minmax', gender=None, age=None):
    """
    1) 매물 벡터를 캐시에서 로드
    2) 정규화 (minmax 또는 zscore)
    3) 가중치 적용
    4) 코사인 유사도 계산 후 후보군 필터링
    5) MMR로 후처리 (옵션)
    6) 카테고리 우선순위 적용 (옵션) → 최종 추천 결과 반환
    """
    logger.info("[recommend_properties] 함수 호출. 파라미터 user_scores=%s, top_n=%d, gender=%s, age=%s",
                user_scores, top_n, gender, age)
    
    category_names = ['transport', 'restaurant', 'health', 'convenience', 'cafe', 'chicken', 'leisure'] 

    # 1) 매물 벡터 로드
    property_array, property_ids = load_property_vectors()
    if property_array is None or property_array.shape[0] == 0:
        logger.warning("[recommend_properties] 매물 정보가 없으므로 빈 리스트([]) 반환합니다.")
        return []
    
    logger.info("[recommend_properties] 캐시에서 %d개 매물을 로드했습니다.", property_array.shape[0])

    # 2) 카테고리별 기본 가중치
    category_weights = np.array([1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0])

    # 성별/연령대별 가중치 추가 적용
    if gender is not None and age is not None:
        age_group = get_age_group(age)
        logger.info("[recommend_properties] 나이 → 연령대 변환: %s → %s, 성별=%s", age, age_group, gender)
        adjustments = get_demographic_weight_adjustments(gender, age_group)
        category_weights = category_weights + adjustments
        logger.info("[recommend_properties] 인구통계 가중치 적용 후: %s", category_weights)
    else:
        logger.info("[recommend_properties] 성별 또는 나이가 입력되지 않아 추가 가중치 적용 안 함.")
    
    # 사용자 ID 추출 (선호도 적용)
    user_id = None
    if user_scores and isinstance(user_scores, dict):
        user_id = user_scores.get("user_id") or user_scores.get("userId")
    
    if user_id:
        preference_weights = get_user_preference_weights(user_id)
        logger.info("[recommend_properties] 사용자 선호도 가중치: %s", preference_weights)
        category_weights = category_weights + preference_weights
        logger.info("[recommend_properties] 선호도 적용 후 category_weights: %s", category_weights)
    else:
        logger.info("[recommend_properties] user_id가 없어, 사용자 선호도 가중치 적용 안 함.")
    
    # 3) 정규화 (minmax / zscore)
    if normalization_method == 'minmax':
        min_max_values = get_category_min_max_values()
        if not min_max_values:
            logger.warning("[recommend_properties] min-max 값을 가져오지 못했으므로 빈 리스트 반환.")
            return []
        
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
        denom[denom == 0] = 1  # 분모가 0이 되지 않도록 처리
        
        norm_array = (property_array - mins) / denom
        logger.info("[recommend_properties] min-max 정규화 수행 완료.")
        
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

    elif normalization_method == 'zscore':
        mean_std_values = get_category_mean_std_values()
        if not mean_std_values:
            logger.warning("[recommend_properties] 평균/표준편차 값을 가져오지 못했으므로 빈 리스트 반환.")
            return []
        
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
        logger.info("[recommend_properties] z-score 정규화 수행 완료.")
        
        user_vals = np.array([
            user_scores.get("transportScore", 0),
            user_scores.get("restaurantScore", 0),
            user_scores.get("healthScore", 0),
            user_scores.get("convenienceScore", 0),
            user_scores.get("cafeScore", 0),
            user_scores.get("chickenScore", 0),
            user_scores.get("leisureScore", 0),
        ])
        user_vector = ((user_vals - means) / stds).reshape(1, -1)

    else:
        logger.error("[recommend_properties] 알 수 없는 정규화 방식: %s", normalization_method)
        return []

    # 4) 가중치 적용
    norm_array = norm_array * category_weights
    user_vector = user_vector * category_weights
    
    logger.info("[recommend_properties] 첫 번째 매물 벡터(정규화+가중치): %s", norm_array[0] if len(norm_array) > 0 else "None")
    logger.info("[recommend_properties] 사용자 벡터(정규화+가중치): %s", user_vector)

    # 5) 코사인 유사도 계산
    similarities = cosine_similarity(norm_array, user_vector).flatten()
    logger.info("[recommend_properties] 총 %d개 매물에 대한 유사도를 계산했습니다.", len(similarities))

    # 상위 후보군 필터링
    top_k = min(1000, len(similarities))
    candidate_order = np.argsort(similarities)[-top_k:].tolist()  # 가장 유사도가 높은 top_k
    candidate_similarities = np.array([similarities[i] for i in candidate_order])
    candidate_vectors = norm_array[candidate_order]

    # 6) MMR 적용 여부
    if apply_mmr_flag:
        selected_candidate_indices = apply_mmr(candidate_similarities, candidate_vectors, top_n, diversity_lambda)
        final_selected_indices = [candidate_order[i] for i in selected_candidate_indices]
        
        # 카테고리 우선순위 적용 (동점 처리)
        if gender is not None and age is not None:
            age_group = get_age_group(age)
            category_priority = get_category_priority(gender, age_group)
            
            top_properties = []
            for i in final_selected_indices:
                property_vector = norm_array[i]
                max_idx = np.argmax(property_vector)
                
                # 동일한 최대값이 여러 개라면 우선순위로 결정
                max_val = property_vector[max_idx]
                max_indices = np.where(property_vector == max_val)[0]
                
                if len(max_indices) > 1:
                    priorities = [category_priority[idx] for idx in max_indices]
                    max_idx = max_indices[np.argmax(priorities)]
                    logger.info("[recommend_properties] 동일 최고값 카테고리=%s → 우선순위 고려. 최종 카테고리 인덱스=%d (%s)",
                                max_indices, max_idx, category_names[max_idx])
                
                top_properties.append({
                    "propertyId": property_ids[i],
                    "similarity": float(similarities[i]),
                    "maxType": category_names[max_idx]
                })
            
            logger.info("[recommend_properties] MMR+우선순위 적용 후 추천 결과 (상위 %d개): %s",
                        top_n, [(p["propertyId"], p["maxType"]) for p in top_properties])
        else:
            top_properties = [{
                "propertyId": property_ids[i],
                "similarity": float(similarities[i])
            } for i in final_selected_indices]
            logger.info("[recommend_properties] MMR 적용 후 추천 결과 (상위 %d개): %s",
                        top_n, [property_ids[i] for i in final_selected_indices])
    else:
        # MMR 미적용 → 단순 유사도 정렬
        properties_rec = [
            {"propertyId": property_ids[i], "similarity": float(similarities[i])}
            for i in range(len(similarities))
        ]
        top_properties = sorted(properties_rec, key=lambda x: x["similarity"], reverse=True)[:top_n]
        logger.info("[recommend_properties] MMR 미적용. 상위 %d개 추천: %s",
                    top_n, [p["propertyId"] for p in top_properties])
    
    # 최종 결과가 비었는지 체크
    if not top_properties:
        logger.warning("[recommend_properties] 추천 결과가 비어 있습니다.")
    else:
        logger.info("[recommend_properties] 최종 추천 개수: %d", len(top_properties))
    
    return top_properties
