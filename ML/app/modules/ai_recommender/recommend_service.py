import pandas as pd
import time
import numpy as np
from datetime import datetime, timedelta
from sqlalchemy import text
import logging
from sklearn.metrics.pairwise import cosine_similarity

from app.config.elasticsearch import get_es_client  # ES 클라이언트를 생성하는 함수
from app.modules.ai_recommender.svd_model import RecommenderModel  # SVD 기반 추천 모델 클래스
from app.modules.ai_recommender.action_score import ACTION_SCORE  # 업데이트된 ACTION_SCORE 매핑 (zzim, search, compare, comment 포함)
from app.config.database import SessionLocal

# 콘텐츠 기반 추천: 외부 모듈의 recommend_properties 함수 사용
from app.modules.content_based.services.recommend_service import recommend_properties

# 로거 설정
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
if not logger.handlers:
    handler = logging.StreamHandler()
    formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    handler.setFormatter(formatter)
    logger.addHandler(handler)

# Elasticsearch 클라이언트와 SVD 모델 인스턴스 초기화
es = get_es_client()
model = RecommenderModel()

def fetch_logs_from_es():
    """
    ES에서 최근 30일간의 로그 데이터를 조회하여,
    필요한 컬럼(userId, propertyId, action, dongId)을 포함한 DataFrame으로 반환합니다.
    """
    logger.info("Fetching logs from ES (last 30 days)...")
    query = {
        "query": {
            "range": {
                "time": {"gte": "now-30d/d"}
            }
        },
        "_source": ["userId", "propertyId", "action", "dongId"]
    }
    result = es.search(index="logs", body=query, size=10000, scroll="2m")
    docs = [doc["_source"] for doc in result["hits"]["hits"]]
    df = pd.DataFrame(docs)
    logger.info("Fetched %d logs from ES.", len(df))
    df['score'] = df['action'].map(ACTION_SCORE)
    # 컬럼명을 camelCase로 유지
    df.rename(columns={"userId": "userId", "propertyId": "propertyId", "dongId": "dongId"}, inplace=True)
    return df[['userId', 'propertyId', 'score', 'dongId']]

def train_model():
    """
    ES에서 로그 데이터를 가져와 추천 모델을 학습시킵니다.
    학습된 모델 인스턴스를 반환합니다.
    """
    logger.info("Training model with ES logs...")
    df = fetch_logs_from_es()
    model.train(df)
    logger.info("Model training completed.")
    return model

def get_most_frequent_dong_for_user(userId: int, duration="2h"):
    """
    최근 duration(기본 2시간) 이내, userId가 남긴 로그 중
    action이 "search", "view", "comment", "zzim", "compare" 인 로그를 조회하고,
    로그 수가 5건 미만이면 None을 반환합니다.
    충분하면 가장 많이 등장한 dongId를 반환합니다.
    """
    logger.info("Fetching recent dong logs for user %d in the last %s...", userId, duration)
    query = {
        "query": {
            "bool": {
                "must": [
                    {"term": {"userId": userId}},
                    {"range": {"time": {"gte": f"now-{duration}"}}}
                ],
                "should": [
                    {"term": {"action": "search"}},
                    {"term": {"action": "view"}},
                    {"term": {"action": "comment"}},
                    {"term": {"action": "zzim"}},
                    {"term": {"action": "compare"}}
                ],
                "minimum_should_match": 1
            }
        },
        "size": 10000,
        "_source": ["dongId"]
    }
    res = es.search(index="logs", body=query, scroll="2m")
    docs = [doc["_source"] for doc in res["hits"]["hits"] if doc["_source"].get("dongId")]
    if len(docs) < 5:
        logger.info("Insufficient dong logs for user %d. Found only %d logs.", userId, len(docs))
        return None
    df = pd.DataFrame(docs)
    most_common = df['dongId'].value_counts().idxmax()
    logger.info("User %d most frequent dong: %s", userId, most_common)
    return most_common

def recommend_by_dong(userId: int, top_k=10):
    """
    사용자의 최근 2시간 로그에서 가장 많이 등장한 dongId를 추출하고,
    해당 dong에 속한 매물들에 대해 SVD 예측을 수행하여 추천 propertyId 리스트를 반환합니다.
    만약 dong 로그가 없거나 후보가 부족하면 빈 리스트를 반환합니다.
    """
    logger.info("Starting dong-based recommendation for user %d...", userId)
    targetDong = get_most_frequent_dong_for_user(userId, duration="2h")
    if targetDong is None:
        logger.info("No sufficient dong logs for user %d.", userId)
        return []
    
    df = fetch_logs_from_es()
    candidates_df = df[df['dongId'] == targetDong]
    if candidates_df.empty:
        logger.info("No candidate properties found in dong %s.", targetDong)
        return []
    
    seen = df[df['userId'] == userId]['propertyId'].unique()
    candidate_items = candidates_df['propertyId'].unique()
    unseen = [pid for pid in candidate_items if pid not in seen]
    if not unseen:
        unseen = candidate_items
    logger.info("Found %d candidate properties in dong %s for user %d.", len(unseen), targetDong, userId)
    
    predictions = [(pid, model.predict(userId, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)
    recommended = [int(pid) for pid, _ in predictions[:top_k]]
    logger.info("Dong-based recommendations for user %d: %s", userId, recommended)
    return recommended

def recommend(userId: int, top_k=10):
    """
    기본 SVD 협업 필터링 추천:
    해당 사용자가 이미 본 매물을 제외한 unseen 매물에 대해
    예측 점수를 계산하여 상위 top_k propertyId 리스트를 반환합니다.
    """
    logger.info("Starting default SVD recommendation for user %d...", userId)
    df = fetch_logs_from_es()
    seen = df[df['userId'] == userId]['propertyId'].unique()
    all_items = df['propertyId'].unique()
    unseen = [pid for pid in all_items if pid not in seen]
    logger.info("User %d has seen %d properties; %d unseen candidates.", userId, len(seen), len(unseen))
    
    predictions = [(pid, model.predict(userId, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)
    recommended = [int(pid) for pid, _ in predictions[:top_k]]
    logger.info("Default recommendations for user %d: %s", userId, recommended)
    return recommended

def get_user_preferences_from_db(userId: int):
    """
    user_preference 테이블에서 해당 userId의 선호 데이터를 조회합니다.
    (transport, restaurant, health, convenience, cafe, leisure) 중 값이 1이면 1, 아니면 0.
    'chicken'은 테이블에 없으므로 기본값 0으로 설정합니다.
    """
    session = SessionLocal()
    try:
        query = text(
            "SELECT transport, restaurant, health, convenience, cafe, leisure "
            "FROM user_preference WHERE user_id = :userId"
        )
        result = session.execute(query, {"userId": userId}).fetchone()
        if result:
            prefs = dict(result._mapping)
            processed = {k: 1 if float(v) == 1 else 0 for k, v in prefs.items()}
            processed["chicken"] = 0
            return processed
        else:
            return None
    finally:
        session.close()

def recommend_for_mainpage(userId: int, top_k=10, gender=None, age=None, user_preferences=None):
    """
    메인페이지 추천:
      - 최근 2시간 내의 로그를 기반으로 dongId를 추출하고, 로그 수가 충분해야만 추천을 수행합니다.
      - 로그 수가 부족하면 추천을 수행하지 않고 빈 결과를 반환합니다.
    """
    logger.info("Starting mainpage recommendation for user %d...", userId)
    targetDong = get_most_frequent_dong_for_user(userId, duration="2h")
    if targetDong is None:
        logger.info("Insufficient log data for user %d. No recommendation will be provided.", userId)
        return {"dongId": None, "propertyIds": []}
    
    # dong 기반 추천 수행
    recs = recommend_by_dong(userId, top_k=top_k)
    return {"dongId": targetDong, "propertyIds": recs}

def load_property_vectors():
    """
    매물 벡터와 propertyId를 DB에서 읽어 글로벌 캐시에 저장합니다.
    캐시 TTL 내라면 기존 캐시를 반환합니다.
    """
    global PROPERTY_VECTORS_CACHE, PROPERTY_IDS_CACHE, PROPERTY_CACHE_TIMESTAMP
    current_time = time.time()
    if PROPERTY_VECTORS_CACHE is not None and (current_time - PROPERTY_CACHE_TIMESTAMP < 300):
        logger.info("Using cached property vectors.")
        return PROPERTY_VECTORS_CACHE, PROPERTY_IDS_CACHE

    logger.info("Loading property vectors from DB...")
    session = SessionLocal()
    try:
        query = text(
            "SELECT property_id, transport_score, restaurant_score, health_score, "
            "convenience_score, cafe_score, chicken_score, leisure_score "
            "FROM property_score"
        )
        results = session.execute(query).fetchall()
        if not results:
            logger.info("No property scores found in DB.")
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

def get_recommendations_for_user(userId: int):
    """
    user 테이블에서 userId에 해당하는 사용자의 정보를 조회한 후,
    recommend_for_mainpage 함수에 전달하여 추천 결과를 반환합니다.
    """
    session = SessionLocal()
    try:
        query = text("""
            SELECT idx, gender, age
            FROM user
            WHERE idx = :userId
        """)
        result = session.execute(query, {"userId": userId}).fetchone()
        if not result:
            return {"error": "User not found"}
        user = dict(result)
    finally:
        session.close()

    return recommend_for_mainpage(
        userId=user["idx"],
        top_k=5,
        gender=user.get("gender", 0),
        age=user.get("age", 0)
    )
