import pandas as pd
import time
import numpy as np
from datetime import datetime, timedelta
from sqlalchemy import text
import logging
from sklearn.metrics.pairwise import cosine_similarity

from app.config.elasticsearch import get_es_client  # ES 클라이언트를 생성하는 함수
from app.modules.ai_recommender.svd_model import RecommenderModel  # 위에서 수정한 SVD 모델 클래스
from app.modules.ai_recommender.action_score import ACTION_SCORE  # 업데이트된 ACTION_SCORE 매핑 (zzim, search, compare, comment 포함)
from app.config.database import SessionLocal

from app.modules.content_based.services.recommend_service import recommend_properties  # 콘텐츠 기반 추천 (사용 시)

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
if not logger.handlers:
    handler = logging.StreamHandler()
    formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    handler.setFormatter(formatter)
    logger.addHandler(handler)

# 전역 Elasticsearch 클라이언트와 SVD 모델 인스턴스
es = get_es_client()
model = RecommenderModel()

PROPERTY_VECTORS_CACHE = None
PROPERTY_IDS_CACHE = None
PROPERTY_CACHE_TIMESTAMP = 0

def fetch_logs_from_es(days=30, size=10000):
    """
    ES에서 최근 'days' 일간의 로그 데이터를 조회하여,
    [userId, propertyId, action, dongId, score] 컬럼을 갖는 DataFrame 반환.
    (단순 검색으로 10,000개까지만 가져옴, 더 많으면 scroll 사용 필요)
    """
    logger.info("Fetching logs from ES (last %d days)...", days)
    query = {
        "query": {
            "range": {
                "time": {"gte": f"now-{days}d/d"}
            }
        },
        "_source": ["userId", "propertyId", "action", "dongId"],
        "size": size,
        "scroll": "2m"
    }
    result = es.search(index="logs", body=query, size=size, scroll="2m")
    docs = [doc["_source"] for doc in result["hits"]["hits"]]
    df = pd.DataFrame(docs)
    if df.empty:
        logger.info("No logs found in ES query.")
        return pd.DataFrame(columns=["userId", "propertyId", "score", "dongId"])

    # action_score를 매핑하여 score 컬럼 추가
    df['score'] = df['action'].map(ACTION_SCORE).fillna(0)
    return df[['userId', 'propertyId', 'score', 'dongId']]

def train_model():
    """
    ES에서 로그 데이터를 가져와 추천 모델(SVD)을 학습.
    에러 없이 학습되면 model.is_trained() = True 상태가 됨.
    """
    logger.info("Starting model training...")
    df = fetch_logs_from_es(days=30)
    if df.empty:
        logger.warning("No training data found. Skipping SVD model training.")
        return

    try:
        model.train(df)
        logger.info("SVD model training completed.")
    except ValueError as e:
        logger.error("Error in model training: %s", e)

def get_most_frequent_dong_for_user(userId: int, duration="2h"):
    """
    최근 duration(기본 2시간) 이내의 로그 중에서
    action이 [search, view, comment, zzim, compare]인 dongId를 집계해서
    가장 자주 등장한 dongId 반환. 로그가 5건 미만이면 None.
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
    dong 기반 추천:
      1) 사용자의 최근 2시간 로그에서 가장 많이 등장한 dongId 찾기
      2) 해당 dong에 있는 매물 중, 사용자가 아직 안 본 매물에 대한 SVD 예측 → 상위 top_k 반환
    """
    logger.info("Starting dong-based recommendation for user %d...", userId)
    targetDong = get_most_frequent_dong_for_user(userId, duration="2h")
    if targetDong is None:
        logger.info("No sufficient dong logs for user %d. Return empty list.", userId)
        return []

    # dongId == targetDong인 로그만 추출 → 후보 매물
    df = fetch_logs_from_es(days=30)
    candidates_df = df[df['dongId'] == targetDong]
    if candidates_df.empty:
        logger.info("No candidate properties found in dong %s.", targetDong)
        return []

    # 사용자가 이미 본 매물은 제외
    seen = df[df['userId'] == userId]['propertyId'].unique()
    candidate_items = candidates_df['propertyId'].unique()
    unseen = [pid for pid in candidate_items if pid not in seen]
    if not unseen:
        # 모두 본 매물이라면, 그냥 전체 candidate_items 쓰거나 빈 리스트
        unseen = candidate_items

    logger.info("Found %d candidate properties in dong %s for user %d.", len(unseen), targetDong, userId)

    # **SVD 예측** (이때 model이 학습되었는지 반드시 확인 필요)
    if not model.is_trained():
        raise ValueError("모델이 아직 초기화되지 않았습니다. 먼저 train()을 호출하세요.")

    predictions = [(pid, model.predict(userId, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)

    recommended = [int(pid) for pid, _ in predictions[:top_k]]
    logger.info("Dong-based recommendations for user %d: %s", userId, recommended)
    return recommended

def recommend(userId: int, top_k=10):
    """
    기본 SVD 협업 필터링:
    userId가 이미 본 매물(seen)은 제외, 나머지 unseen 매물에 대해 SVD로 예측 후 상위 top_k 반환
    """
    logger.info("Starting default SVD recommendation for user %d...", userId)
    # logs df
    df = fetch_logs_from_es(days=30)
    seen = df[df['userId'] == userId]['propertyId'].unique()
    all_items = df['propertyId'].unique()
    unseen = [pid for pid in all_items if pid not in seen]
    logger.info("User %d has seen %d properties; unseen candidates: %d.", userId, len(seen), len(unseen))

    # 모델이 학습되지 않았다면 예외
    if not model.is_trained():
        raise ValueError("모델이 아직 초기화되지 않았습니다. 먼저 train()을 호출하세요.")

    predictions = [(pid, model.predict(userId, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)

    recommended = [int(pid) for pid, _ in predictions[:top_k]]
    logger.info("Default recommendations for user %d: %s", userId, recommended)
    return recommended

def get_user_preferences_from_db(userId: int):
    """
    user_preference 테이블에서 userId 선호도 조회 (transport, restaurant, health, ...)
    (현재 예제에서 사용되지 않지만, 추후 콘텐츠 기반 가중치 등에 활용 가능)
    """
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
            processed["chicken"] = 0  # chicken 기본값 0
            return processed
        else:
            return None
    finally:
        session.close()

def recommend_for_mainpage(userId: int, top_k=10, gender=None, age=None, user_preferences=None):
    """
    메인페이지 추천:
      1) dong 기반 추천 시도
      2) dong 로그가 충분치 않다면 fallback (ex. default SVD 등)
    """
    logger.info("Starting mainpage recommendation for user %d...", userId)

    # dong 로그가 충분한지 확인
    targetDong = get_most_frequent_dong_for_user(userId, duration="2h")
    if targetDong is None:
        logger.info("Fallback to default SVD recommendation for user %d.", userId)
        recs = recommend(userId, top_k=top_k)
        return {"dongId": None, "propertyIds": recs}

    # dong 기반 추천 수행
    recs = recommend_by_dong(userId, top_k=top_k)
    return {"dongId": targetDong, "propertyIds": recs}

def load_property_vectors():
    """
    (콘텐츠 기반 추천용) 매물 벡터 + property_id를 DB에서 읽어 캐싱
    현재 예제에선 사용 안 하지만, 필요 시 활용.
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
    user 테이블에서 userId 정보를 가져와, recommend_for_mainpage에 전달하여 최종 추천을 수행.
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
            return {"error": f"User {userId} not found"}
        user = dict(result)
    finally:
        session.close()

    # 메인페이지 추천
    output = recommend_for_mainpage(
        userId=user["idx"],
        top_k=5,
        gender=user.get("gender", 0),
        age=user.get("age", 0),
    )
    return output
