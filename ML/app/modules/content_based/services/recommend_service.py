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

def load_property_vectors():
    """
    매물 벡터와 property_id를 DB에서 읽어 글로벌 캐시에 저장합니다.
    캐시 TTL 내라면 기존 캐시를 반환합니다.
    """
    global PROPERTY_VECTORS_CACHE, PROPERTY_IDS_CACHE, PROPERTY_CACHE_TIMESTAMP
    current_time = time.time()
    if PROPERTY_VECTORS_CACHE is not None and (current_time - PROPERTY_CACHE_TIMESTAMP < PROPERTY_CACHE_TTL):
        logger.info("Using cached property vectors.")
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
            logger.info("No property scores found.")
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
        PROPERTY_CACHE_TIMESTAMP = current_time
        logger.info("Loaded %d property vectors into cache.", property_array.shape[0])
        return property_array, property_ids
    except Exception as e:
        logger.error("Error loading property vectors: %s", e)
        return None, None
    finally:
        session.close()

def apply_mmr(similarities, property_vectors, top_n, diversity_lambda=0.5):
    """
    MMR을 적용하여 추천 결과의 다양성을 높입니다.
    
    :param similarities: 사용자 벡터와 각 매물 벡터 간의 코사인 유사도 (array)
    :param property_vectors: 정규화 및 가중치가 적용된 매물 벡터 (shape: [n_properties, n_features])
    :param top_n: 최종 선택할 매물의 개수
    :param diversity_lambda: 관련성과 다양성의 균형 (0~1)
    :return: 선택된 매물의 인덱스 리스트
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

# Edited By 전희성
def get_age_group(age):
    """
    나이를 연령대로 변환한다.
    
    :param age: 정수형 나이
    :return: 연령대 문자열 ('20s', '30s', '40s', '50s_plus')
    50세 이상은 모두 동일한 값으로 저장
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
            logger.warning("Age %d is below 20, defaulting to '20s'", age)
            return '20s'
    except (ValueError, TypeError):
        logger.warning("Invalid age value: %s, defaulting to '30s'", age)
        return '30s'

# Edited By 전희성
def convert_gender_code(gender_code):
    """
    쿠키에는 성별 데이터가 0/1로 넘어오기에 이를 문자열로 알아보기 쉽게 변환한다.
    
    :param gender_code: 정수 성별 코드 (1: male, 0: female)
    :return: 'male' 또는 'female'
    """
    if gender_code == 1:
        return 'male'
    elif gender_code == 0:
        return 'female'
    else:
        logger.warning("Unknown gender code: %d, defaulting to 'male'", gender_code)
        return 'male'

# Edited By 전희성
def get_demographic_weight_adjustments(gender, age_group):
    """
    성별과 연령대에 따른 카테고리별 가중치 조정값을 반환합니다.
    기본 가중치에 이 조정값을 더하여 최종 가중치를 계산합니다.
    
    :param gender: 성별 (문자열 'male'/'female' 또는 정수 코드 0/1)
    :param age_group: '20s', '30s', '40s', '50s_plus'
    :return: 카테고리별 가중치 조정값 (순서: transport, restaurant, health, convenience, cafe, chicken, leisure)
    """
    # 정수형 성별 코드를 문자열로 변환
    if isinstance(gender, int):
        gender = convert_gender_code(gender)
    elif not isinstance(gender, str):
        logger.warning("Unexpected gender type: %s, defaulting to 'male'", type(gender))
        gender = 'male'
    # 기본 조정값
    default_adjustments = np.array([0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0])
    
    # 성별/연령대별 가중치 조정값 (순서: transport, restaurant, health, convenience, cafe, chicken, leisure)
    demographic_adjustments = {
        # 20대
        ('male', '20s'): np.array([0.0, +0.2, -0.2, +0.1, -0.1, +0.3, +0.4]),  # 남성 20대: 여가/치킨 중요↑, 의료↓
        ('female', '20s'): np.array([+0.1, +0.1, -0.1, 0.0, +0.5, 0.0, +0.2]), # 여성 20대: 카페 매우 중요↑, 여가↑
        
        # 30대
        ('male', '30s'): np.array([-0.2, +0.1, 0.0, 0.0, 0.0, +0.1, +0.2]),   # 남성 30대: 교통↓, 여가↑
        ('female', '30s'): np.array([-0.1, 0.0, +0.1, +0.1, +0.3, -0.1, +0.1]), # 여성 30대: 카페↑, 교통↓
        
        # 40대
        ('male', '40s'): np.array([-0.2, 0.0, +0.2, 0.0, -0.1, 0.0, +0.1]),   # 남성 40대: 의료↑, 교통↓
        ('female', '40s'): np.array([-0.1, 0.0, +0.3, +0.1, +0.1, -0.1, 0.0]), # 여성 40대: 의료 매우 중요↑
        
        # 50대 이상
        ('male', '50s_plus'): np.array([-0.1, -0.1, +0.5, +0.1, -0.2, -0.2, +0.2]), # 남성 50대+: 의료 매우 중요↑, 카페/치킨↓
        ('female', '50s_plus'): np.array([0.0, -0.1, +0.5, +0.2, 0.0, -0.2, 0.0]) # 여성 50대+: 의료 매우 중요↑, 편의점↑
    }
    
    # 성별/연령대 조합이 정의되어 있는지 확인
    key = (gender, age_group)
    if key in demographic_adjustments:
        logger.info("성별 및 연령대 별 가중치 적용 for %s, %s: %s", gender, age_group, demographic_adjustments[key])
        return demographic_adjustments[key]
    else:
        logger.warning("성별 및 연령대 조회 중 오류 발생: %s, %s. Using default adjustments.", gender, age_group)
        return default_adjustments

# Edited by 전희성
def get_user_preference_weights(user_id):
    """
    사용자의 선호도 가중치를 DB에서 조회합니다.
    user_preference 테이블에서 사용자가 중요하게 생각하는 항목(값이 1)에 추가 가중치를 줍니다.
    
    :param user_id: 사용자 ID
    :return: 선호도 가중치 (순서: transport, restaurant, health, convenience, cafe, chicken, leisure)
    """
    global USER_PREFERENCE_CACHE, USER_PREFERENCE_CACHE_TIMESTAMP, USER_PREFERENCE_CACHE_TTL
    current_time = time.time()
    
    # 캐시에 있고 TTL 내라면 캐시된 값 반환
    if user_id in USER_PREFERENCE_CACHE and (current_time - USER_PREFERENCE_CACHE_TIMESTAMP < USER_PREFERENCE_CACHE_TTL):
        logger.info("Using cached user preference for user_id: %s", user_id)
        return USER_PREFERENCE_CACHE[user_id]
    
    # 기본 가중치 조정값 (추가 가중치 없음)
    default_preference = np.array([0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0])
    
    if not user_id:
        logger.info("User ID not provided. Using default preference weights.")
        return default_preference
    
    # DB에서 사용자 선호도 조회
    session = SessionLocal()
    try:
        query = text("""
            SELECT 
                transport_preference, restaurant_preference, health_preference, 
                convenience_preference, cafe_preference, chicken_preference, leisure_preference
            FROM user_preference
            WHERE user_id = :user_id
        """)
        result = session.execute(query, {"user_id": user_id}).fetchone()
        
        if not result:
            logger.info("No preference data found for user_id: %s. Using default.", user_id)
            return default_preference
        
        # 선호도가 1인 항목에 추가 가중치 1.0 적용
        preference_adjustments = np.array([
            1.0 if result._mapping["transport_preference"] == 1 else 0.0,
            1.0 if result._mapping["restaurant_preference"] == 1 else 0.0,
            1.0 if result._mapping["health_preference"] == 1 else 0.0,
            1.0 if result._mapping["convenience_preference"] == 1 else 0.0,
            1.0 if result._mapping["cafe_preference"] == 1 else 0.0,
            1.0 if result._mapping["chicken_preference"] == 1 else 0.0,
            1.0 if result._mapping["leisure_preference"] == 1 else 0.0
        ])
        
        # 캐시 업데이트
        USER_PREFERENCE_CACHE[user_id] = preference_adjustments
        USER_PREFERENCE_CACHE_TIMESTAMP = current_time
        
        logger.info("사용자(%s) 선호도 가중치 적용: %s", user_id, preference_adjustments)
        return preference_adjustments
    
    except Exception as e:
        logger.error("Error fetching user preference: %s", e)
        return default_preference
    finally:
        session.close()

# Edited by 전희성
def get_category_priority(gender, age_group):
    """
    성별과 연령대에 따른 카테고리 우선순위를 반환합니다.
    가중치가 동일한 경우 이 우선순위를 통해 선택할 카테고리를 결정합니다.
    
    :param gender: 성별 (문자열 'male'/'female' 또는 정수 코드 0/1)
    :param age_group: '20s', '30s', '40s', '50s_plus'
    :return: 카테고리 우선순위 (순서: transport, restaurant, health, convenience, cafe, chicken, leisure)
              숫자가 클수록 우선순위가 높음
    """
    # 정수형 성별 코드를 문자열로 변환
    if isinstance(gender, int):
        gender = convert_gender_code(gender)
    elif not isinstance(gender, str):
        logger.warning("Unexpected gender type: %s, defaulting to 'male'", type(gender))
        gender = 'male'
    
    # 기본 우선순위 (숫자가 클수록 우선순위 높음)
    default_priority = np.array([4, 5, 3, 2, 6, 1, 7])
    
    # 성별/연령대별 카테고리 우선순위 (순서: transport, restaurant, health, convenience, cafe, chicken, leisure)
    category_priorities = {
        # 20대
        ('male', '20s'): np.array([3, 5, 1, 2, 4, 6, 7]),  # 남성 20대: 여가 > 치킨 > 식당 > 카페 > 교통 > 편의점 > 의료
        ('female', '20s'): np.array([3, 4, 1, 2, 7, 5, 6]), # 여성 20대: 카페 > 여가 > 치킨 > 식당 > 교통 > 편의점 > 의료
        
        # 30대
        ('male', '30s'): np.array([2, 5, 3, 4, 6, 1, 7]),   # 남성 30대: 여가 > 카페 > 식당 > 편의점 > 의료 > 교통 > 치킨
        ('female', '30s'): np.array([2, 4, 5, 3, 7, 1, 6]), # 여성 30대: 카페 > 여가 > 의료 > 식당 > 편의점 > 교통 > 치킨
        
        # 40대
        ('male', '40s'): np.array([2, 4, 7, 3, 1, 5, 6]),   # 남성 40대: 의료 > 여가 > 치킨 > 식당 > 편의점 > 교통 > 카페
        ('female', '40s'): np.array([2, 3, 7, 5, 6, 1, 4]), # 여성 40대: 의료 > 카페 > 편의점 > 여가 > 식당 > 교통 > 치킨
        
        # 50대 이상
        ('male', '50s_plus'): np.array([3, 2, 7, 5, 1, 4, 6]), # 남성 50대+: 의료 > 여가 > 교통 > 치킨 > 편의점 > 식당 > 카페
        ('female', '50s_plus'): np.array([3, 2, 7, 6, 4, 1, 5]) # 여성 50대+: 의료 > 편의점 > 여가 > 카페 > 교통 > 식당 > 치킨
    }
    
    # 성별/연령대 조합이 정의되어 있는지 확인
    key = (gender, age_group)
    if key in category_priorities:
        logger.info("성별 및 연령대 별 카테고리 우선순위 적용 for %s, %s: %s", gender, age_group, category_priorities[key])
        return category_priorities[key]
    else:
        logger.warning("성별 및 연령대 우선순위 조회 중 오류 발생: %s, %s. Using default priority.", gender, age_group)
        return default_priority

def recommend_properties(user_scores: dict, top_n=5, apply_mmr_flag=True, diversity_lambda=0.5, normalization_method='minmax', gender=None, age=None):
    """
    글로벌 캐시에서 매물 벡터를 로드한 후, 정규화( minmax 또는 zscore ), 가중치 적용 및 후보군 필터링 후 MMR 후처리를 수행합니다.
    
    :param user_scores: 예) {"transport_score": 0.5, "restaurant_score": 0.5, ...}
    :param top_n: 추천할 매물 수
    :param apply_mmr_flag: MMR 후처리 적용 여부
    :param diversity_lambda: MMR 관련성 vs 다양성 균형 파라미터 (0~1)
    :param normalization_method: 'minmax' 또는 'zscore'
    :return: 추천된 매물 리스트
    """

    # (전희성) 0. 카테고리 이름 리스트 추가
    category_names = ['transport', 'restaurant', 'health', 'convenience', 'cafe', 'chicken', 'leisure'] 

    # 1. 캐시된 매물 벡터 로드
    property_array, property_ids = load_property_vectors()
    if property_array is None:
        return []
    
    logger.info("Fetched %d properties from cache.", property_array.shape[0])
    
    """
    Todo : 각 성별, 나이대별로 가중치가 다르다.
    예를 들면 20대 남성은 술을 좋아할 수도 있고, 30대 여성은 카페를 좋아할 수도 있다.
    사용자 정보를 받아와서 gender와 age에 따른 가중치 차등 적용이 필요하다.

    pseudo code
    if(나이가 20대 && 성별이 남성) -> category_weights[restaurant] + 0.5
    else if(나이가 20대 && 성별이 여성) -> category_weights[cafe] + 0.7,
    ....
    """

    # 2. 카테고리별 가중치 (순서: transport, restaurant, health, convenience, cafe, chicken, leisure)
    category_weights = np.array([1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0])
    
    # 3. 인구통계별 가중치 조정값 적용
    if gender is not None and age is not None:
        # 나이를 연령대로 변환
        age_group = get_age_group(age)
        logger.info("사용자의 나이: %s 를 나이대로 변환: %s", age, age_group)
        
        # 성별과 연령대에 따른 가중치 조정값 가져오기
        adjustments = get_demographic_weight_adjustments(gender, age_group)
        
        # 기본 가중치에 조정값 적용
        category_weights = category_weights + adjustments
        logger.info("기존 가중치에 성별/나이대별 가중치 적용 => %s", category_weights)
    else:
        logger.info("에러로 인한 기본 가중치 적용... %s", category_weights)
        
    # 4. user_scores에서 user_id 추출하여 사용자 선호도 반영
    # user_scores는 API에서 전달받는 값으로 필요한 정보를 포함할 수 있음
    user_id = None
    if user_scores and isinstance(user_scores, dict) and "user_id" in user_scores:
        user_id = user_scores.get("user_id")
        
    # 사용자 선호도 가중치 적용 (user_id가 있는 경우)
    if user_id:
        preference_weights = get_user_preference_weights(user_id)
        category_weights = category_weights + preference_weights
        logger.info("사용자 선호도 가중치 추가 적용 => %s", category_weights)

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
        # 사용자 점수는 이미 0~1 범위라고 가정
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
    elif normalization_method == 'zscore':
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
    else:
        logger.error("Unknown normalization method: %s", normalization_method)
        return []

    # 3. 가중치 적용
    norm_array = norm_array * category_weights
    user_vector = user_vector * category_weights
    
    logger.info("First property vector (normalized & weighted): %s", norm_array[0])
    logger.info("User vector (normalized & weighted): %s", user_vector)
    
    # 4. 코사인 유사도 계산
    similarities = cosine_similarity(norm_array, user_vector).flatten()
    logger.info("Calculated similarities for %d properties.", len(similarities))
    
    # 5. 상위 후보군 (예: top 1000) 필터링 후 MMR 적용
    top_k = min(1000, len(similarities))
    candidate_order = np.argsort(similarities)[-top_k:].tolist()  # 상위 top_k 인덱스
    candidate_similarities = np.array([similarities[i] for i in candidate_order])
    candidate_vectors = norm_array[candidate_order]
    
    if apply_mmr_flag:
        selected_candidate_indices = apply_mmr(candidate_similarities, candidate_vectors, top_n, diversity_lambda)
        # 원래 인덱스로 변이 한다
        final_selected_indices = [candidate_order[i] for i in selected_candidate_indices]
        
        # 카테고리 우선순위 적용 (점수가 같은 경우 처리)
        if gender is not None and age is not None:
            age_group = get_age_group(age)
            category_priority = get_category_priority(gender, age_group)
            
            # 우선순위를 적용하여 동일 점수 카테고리 정렬
            # 각 property에 대해 similarity와 priority를 함께 고려
            top_properties = []
            for i in final_selected_indices:
                property_vector = norm_array[i]
                max_idx = np.argmax(property_vector)  #여기 기능 가장 높은 점수의 카테고리 인덱스
                
                # 동일한 최대값이 있는지 확인
                max_val = property_vector[max_idx]
                max_indices = np.where(property_vector == max_val)[0]
                
                if len(max_indices) > 1:
                    # 동일한 최대값이 여러 개 있는 경우, 우선순위 적용
                    priorities = [category_priority[idx] for idx in max_indices]
                    max_idx = max_indices[np.argmax(priorities)]
                    logger.info("동일 점수 카테고리 발견: %s, 우선순위 적용하여 %d 카테고리 선택", max_indices, max_idx)
                
                top_properties.append({
                    "propertyId": property_ids[i], 
                    "similarity": float(similarities[i]),
                    "primaryCategory": category_names[max_idx]
                })
            
            logger.info("Top %d recommended properties after MMR with priority: %s", 
                       top_n, [(p["propertyId"], p["primaryCategory"]) for p in top_properties])
        else:
            top_properties = [{"propertyId": property_ids[i], "similarity": float(similarities[i])} for i in final_selected_indices]
            logger.info("Top %d recommended properties after MMR: %s", top_n, [property_ids[i] for i in final_selected_indices])
    else:
        properties_rec = [{"propertyId": property_ids[i], "similarity": float(similarities[i])} for i in range(len(similarities))]
        top_properties = sorted(properties_rec, key=lambda x: x["similarity"], reverse=True)[:top_n]
        logger.info("Top %d recommended properties: %s", top_n, [p["propertyId"] for p in top_properties])
        
    return top_properties