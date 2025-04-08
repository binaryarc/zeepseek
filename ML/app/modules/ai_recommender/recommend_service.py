import pandas as pd
import time
import numpy as np
from datetime import datetime, timedelta
from sqlalchemy import text
import logging
from sklearn.metrics.pairwise import cosine_similarity

from app.config.elasticsearch import get_es_client
from app.modules.ai_recommender.svd_model import RecommenderModel
from app.modules.ai_recommender.action_score import ACTION_SCORE
from app.config.database import SessionLocal
from app.modules.content_based.services.recommend_service import recommend_properties  # 콘텐츠 기반 추천 (필요 시)

# content_based_with_office_location 함수는 별도 모듈에서 import
from app.modules.content_based.services.content_based_with_office import content_based_with_office_location

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
if not logger.handlers:
    handler = logging.StreamHandler()
    formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    handler.setFormatter(formatter)
    logger.addHandler(handler)

# 전역 ES 클라이언트와 SVD 모델
es = get_es_client()
model = RecommenderModel()

PROPERTY_VECTORS_CACHE = None
PROPERTY_IDS_CACHE = None
PROPERTY_CACHE_TIMESTAMP = 0

def fetch_logs_from_es(days: int = 30, size: int = 10000):
    """
    Elasticsearch에서 최근 'days' 일간 로그를 가져와
    [userId, propertyId, action, dongId, computedRoomType, age, gender] -> ACTION_SCORE 매핑 후 DataFrame 반환.
    propertyId가 -1인 데이터는 제거합니다.
    """
    query = {
        "query": {
            "range": {
                "time": {"gte": f"now-{days}d/d"}
            }
        },
        "_source": ["userId", "propertyId", "action", "dongId", "computedRoomType", "age", "gender"]
    }
    result = es.search(
        index="logs",
        body=query,
        size=size,
        scroll="2m"
    )
    docs = [doc["_source"] for doc in result["hits"]["hits"]]
    df = pd.DataFrame(docs)
    # [수정] 만약 특정 컬럼이 누락되면 기본값 None으로 추가
    for col in ["computedRoomType", "age", "gender"]:
        if col not in df.columns:
            df[col] = None
    if df.empty:
        logger.info("No logs found from ES.")
        return pd.DataFrame(columns=["userId", "propertyId", "action", "dongId", "computedRoomType", "age", "gender"])
    df["score"] = df["action"].map(ACTION_SCORE).fillna(0)
    # -1인 propertyId 제거
    df = df[df["propertyId"] != -1]
    logger.info("[fetch_logs_from_es] Retrieved %d logs after filtering propertyId=-1", len(df))
    return df[["userId", "propertyId", "score", "dongId", "computedRoomType", "age", "gender"]]

def train_model():
    """
    ES 로그 데이터를 가져와 SVD 모델 학습
    - 학습 시 userId, propertyId가 -1인 데이터는 제거합니다.
    """
    logger.info("Starting model training...")
    df = fetch_logs_from_es(days=30)
    if df.empty:
        logger.warning("No training data found. Skipping SVD model training.")
        return
    # -1 필터링 (추가로 userId도 -1인 경우 제거)
    df = df[(df["userId"] != -1) & (df["propertyId"] != -1)]
    logger.info("Data after filtering -1 => %d logs remain", len(df))
    if df.empty:
        logger.warning("After filtering -1, no data left. Skipping training.")
        return
    try:
        model.train(df)
        logger.info("SVD model training completed.")
    except ValueError as e:
        logger.error("Error in model training: %s", e)

def get_dominant_computed_room_type(userId: int):
    """
    최근 100개 로그에서 computedRoomType의 빈도를 집계하여
    5건 이상 등장한 dominant computedRoomType을 반환한다.
    """
    logger.info("Fetching computedRoomType for user %d...", userId)
    query = {
        "query": {
            "bool": {
                "must": [
                    {"term": {"userId": userId}}
                ]
            }
        },
        "size": 100,
        "sort": [{"time": {"order": "desc"}}],
        "_source": ["computedRoomType"]
    }
    try:
        res = es.search(index="logs", body=query, scroll="2m")
    except Exception as e:
        logger.warning("Elasticsearch error (computedRoomType): %s", e)
        return None
    docs = [doc["_source"] for doc in res["hits"]["hits"] if doc["_source"].get("computedRoomType")]
    if len(docs) < 5:
        logger.info("Insufficient computedRoomType logs for user %d.", userId)
        return None
    df = pd.DataFrame(docs)
    dominant = df['computedRoomType'].value_counts()
    if dominant.iloc[0] >= 5:
        dominant_type = dominant.idxmax()
        logger.info("User %d dominant computedRoomType: %s", userId, dominant_type)
        return dominant_type
    logger.info("No dominant computedRoomType for user %d.", userId)
    return None

def get_most_frequent_dong_for_user(userId: int):
    """
    최근 100개의 최신 로그에서, 
    action in [search, view, comment, zzim, compare] 조건에 맞는 로그를 대상으로
    dongId 빈도수를 집계하여 가장 자주 등장한 dongId를 반환.
    유효한 dongId가 5건 미만이면 None 반환.
    """
    logger.info("Fetching latest 100 logs for user %d...", userId)
    query = {
        "query": {
            "bool": {
                "must": [
                    {"term": {"userId": userId}}
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
        "size": 100,
        "sort": [
            {"time": {"order": "desc"}}
        ],
        "_source": ["dongId"]
    }
    try:
        res = es.search(index="logs", body=query, scroll="2m")
    except Exception as e:
        logger.warning("Elasticsearch error: %s. Treating as no logs.", e)
        return None
    docs = [doc["_source"] for doc in res["hits"]["hits"] if doc["_source"].get("dongId") not in (None, -1)]
    if len(docs) < 5:
        logger.info("Insufficient dong logs for user %d. Found only %d logs.", userId, len(docs))
        return None
    df = pd.DataFrame(docs)
    most_common = df['dongId'].value_counts().idxmax()
    logger.info("User %d most frequent dong: %s", userId, most_common)
    return most_common

def recommend_by_dong(userId: int, top_k=10):
    logger.info("Starting dong-based recommendation for user %d...", userId)
    targetDong = get_most_frequent_dong_for_user(userId)
    if targetDong is None:
        logger.info("No sufficient dong logs for user %d. Return empty list.", userId)
        return []
    df = fetch_logs_from_es(days=30)
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
    if not model.is_trained():
        raise ValueError("모델이 아직 초기화되지 않았습니다. 먼저 train()을 호출하세요.")
    predictions = [(pid, model.predict(userId, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)
    recommended = [int(pid) for pid, _ in predictions[:top_k]]
    logger.info("Dong-based recommendations for user %d: %s", userId, recommended)
    return recommended

def recommend(userId: int, top_k=10, dong_id: int = None, computed_room_type: str = None):
    logger.info("Starting default SVD recommendation for user %d...", userId)
    df = fetch_logs_from_es(days=30)
    # dong_id 필터링
    if dong_id is not None:
        df = df[df['dongId'] == dong_id]
    # computedRoomType 필터링
    if computed_room_type:
        df = df[df['computedRoomType'] == computed_room_type]
    seen = df[df['userId'] == userId]['propertyId'].unique()
    all_items = df['propertyId'].unique()
    unseen = [pid for pid in all_items if pid not in seen]
    logger.info("User %d has seen %d properties; unseen candidates: %d.", userId, len(seen), len(unseen))
    if not model.is_trained():
        raise ValueError("모델이 아직 초기화되지 않았습니다. 먼저 train()을 호출하세요.")
    predictions = [(pid, model.predict(userId, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)
    recommended = [int(pid) for pid, _ in predictions[:top_k]]
    logger.info("Default recommendations for user %d: %s", userId, recommended)
    return recommended

def get_user_preferences_from_db(userId: int):
    session = SessionLocal()
    try:
        query = text("""
            SELECT transport, restaurant, health, convenience, cafe, leisure
            FROM user_preference
            WHERE user_id = :userId
        """)
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

def hybrid_recommend(user_id: int, top_k=5, dong_id: int = None):
    logger.info("Starting hybrid recommendation for user %d...", user_id)
    # dominant computedRoomType 결정
    dominant_computed_room_type = get_dominant_computed_room_type(user_id)
    # SVD 추천은 dominant computedRoomType 혹은 dong_id가 있으면 해당 조건으로 필터링하도록 함
    try:
        rec_svd = recommend(user_id, top_k, dong_id=dong_id, computed_room_type=dominant_computed_room_type)
    except ValueError as e:
        logger.warning("SVD model not trained: %s. Skipping SVD recommendation.", e)
        rec_svd = []
    try:
        cb_res = content_based_with_office_location(user_id, top_k)
        rec_cb = cb_res.get("propertyIds", [])
    except Exception as e:
        logger.error("Content based recommendation failed: %s", e)
        rec_cb = []
    combined = rec_svd + rec_cb
    merged = list(dict.fromkeys(combined))[:top_k]
    logger.info("Hybrid recommendation for user %d: %s", user_id, merged)
    return merged

def recommend_for_mainpage(userId: int, top_k=10):
    logger.info("Starting mainpage recommendation for user %d...", userId)
    session = SessionLocal()
    try:
        row = session.execute(
            text("SELECT dong_id FROM user_preference WHERE user_id=:uid"),
            {"uid": userId}
        ).fetchone()
        fallbackDong = row._mapping["dong_id"] if row else None
    finally:
        session.close()
        
    targetDong = get_most_frequent_dong_for_user(userId)
    # targetDong이 없으면 fallback 사용
    final_dong = targetDong if targetDong is not None else fallbackDong
    if not targetDong:
        logger.info("No dong logs => fallback hybrid recommendation using user_preference dong_id=%s", fallbackDong)
        recs = hybrid_recommend(userId, top_k, dong_id=fallbackDong)
        final_output = {"dongId": final_dong, "propertyIds": recs}
        logger.info("Final mainpage recommendation: %s", final_output)
        return final_output
    recs = recommend_by_dong(userId, top_k)
    if not recs:
        logger.info("dong-based recommendation empty => fallback hybrid recommendation using user_preference dong_id=%s", fallbackDong)
        recs = hybrid_recommend(userId, top_k, dong_id=fallbackDong)
        final_output = {"dongId": fallbackDong, "propertyIds": recs}
        logger.info("Final mainpage recommendation: %s", final_output)
        return final_output
    final_output = {"dongId": targetDong, "propertyIds": recs}
    logger.info("Final mainpage recommendation: %s", final_output)
    return final_output

def load_property_vectors():
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
    session = SessionLocal()
    try:
        query = text("""
            SELECT idx, gender, age
            FROM user
            WHERE idx = :userId
        """)
        result = session.execute(query, {"userId": userId}).fetchone()
        if not result:
            logger.info("User %d not found", userId)
            return {"error": f"User {userId} not found"}
        user = dict(result)
    finally:
        session.close()
    output = recommend_for_mainpage(
        userId=user["idx"],
        top_k=5
    )
    logger.info("Final recommendations for user %d: %s", userId, output)
    return output
