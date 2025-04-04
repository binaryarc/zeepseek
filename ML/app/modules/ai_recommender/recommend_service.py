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
from app.modules.content_based.services.recommend_service import recommend_properties

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
    [userId, propertyId, action, dongId] -> score 매핑 -> DataFrame
    """
    query = {
        "query": {
            "range": {
                "time": {"gte": f"now-{days}d/d"}
            }
        },
        "_source": ["userId", "propertyId", "action", "dongId"]
    }

    # ES 호출 시 body=query, 파라미터로 size, scroll
    result = es.search(
        index="logs",
        body=query,
        size=size,
        scroll="2m"
    )

    docs = [doc["_source"] for doc in result["hits"]["hits"]]
    df = pd.DataFrame(docs)
    if df.empty:
        logger.info("No logs found from ES.")
        return pd.DataFrame(columns=["userId", "propertyId", "action", "dongId"])

    # action_score 매핑
    df["score"] = df["action"].map(ACTION_SCORE).fillna(0)
    logger.info("[fetch_logs_from_es] Retrieved %d logs", len(df))
    return df[["userId", "propertyId", "score", "dongId"]]


def train_model():
    """
    ES에서 로그를 가져와 SVD 모델 학습
    - userId, propertyId가 -1인 경우는 유효 데이터가 아니므로 제거
    """
    logger.info("Starting model training...")
    df = fetch_logs_from_es(days=30)
    if df.empty:
        logger.warning("No training data found. Skipping SVD model training.")
        return

    # -1 필터링 (userId나 propertyId가 -1이면 학습에서 제외)
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


def get_most_frequent_dong_for_user(userId: int, duration="2h"):
    """
    최근 duration(기본 2시간) 내 userId가 남긴 로그 중에서
    action in [search, view, comment, zzim, compare] 조건
    dongId 빈도수 최댓값
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
    docs = [doc["_source"] for doc in res["hits"]["hits"] if doc["_source"].get("dongId") not in (None, -1)]

    if len(docs) < 5:
        logger.info("Insufficient dong logs for user %d. Found only %d logs.", userId, len(docs))
        return None

    df = pd.DataFrame(docs)
    most_common = df['dongId'].value_counts().idxmax()
    logger.info("User %d most frequent dong: %s", userId, most_common)
    return most_common


def recommend_by_dong(userId: int, top_k=10):
    """
    1) userId의 최근 2시간 'dong 로그' (action in [search, view, ...]) >= 5건 => dongId
    2) dongId 로그 기반 매물 => SVD 예측 => 상위 top_k
    """
    logger.info("Starting dong-based recommendation for user %d...", userId)
    targetDong = get_most_frequent_dong_for_user(userId, duration="2h")
    if targetDong is None:
        logger.info("No sufficient dong logs for user %d. Return empty list.", userId)
        return []

    # dongId == targetDong인 로그만 추출
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
        unseen = candidate_items

    logger.info("Found %d candidate properties in dong %s for user %d.", len(unseen), targetDong, userId)

    # SVD 예측 (학습 검사)
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
    - userId가 이미 본 매물(seen) 제외
    - 남은 unseen 매물에 대해 SVD 예측 => 상위 top_k
    """
    logger.info("Starting default SVD recommendation for user %d...", userId)
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
    user_preference 테이블에서 userId 선호도 조회 (transport, restaurant, ...)
    (현재 예제에서 사용X, 필요 시 콘텐츠 기반 등 다른 로직에 활용)
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


def recommend_for_mainpage(userId: int, top_k=10):
    """
    메인페이지 추천:
      1) dong 기반 추천 시도
      2) dong 로그가 충분치 않거나 dong-based 추천 결과가 없으면 fallback → 하이브리드 추천 방식 사용
      3) 최종 반환: {"dongId": <dong_id>, "propertyIds": [매물ID, ...]}
    """
    logger.info("Starting mainpage recommendation for user %d...", userId)

    # 0) 우선, user_preference 테이블에서 fallbackDong (사용자가 설정한 dong_id) 조회
    session = SessionLocal()
    try:
        row = session.execute(
            text("SELECT dong_id FROM user_preference WHERE user_id=:uid"),
            {"uid": userId}
        ).fetchone()
        fallbackDong = row._mapping["dong_id"] if row else None
    finally:
        session.close()

    # 1) 로그 기반 dong 추천 시도
    targetDong = get_most_frequent_dong_for_user(userId, duration="2h")
    if not targetDong:
        logger.info("No dong logs => fallback hybrid recommendation using user_preference dong_id=%s", fallbackDong)
        recs = hybrid_recommend(userId, top_k)
        return {"dongId": fallbackDong, "propertyIds": recs}

    # 2) dong 기반 추천
    recs = recommend_by_dong(userId, top_k)
    if not recs:
        logger.info("dong-based recommendation empty => fallback hybrid recommendation using user_preference dong_id=%s", fallbackDong)
        recs = hybrid_recommend(userId, top_k)
        return {"dongId": fallbackDong, "propertyIds": recs}

    # 3) dong 기반 추천 결과가 있다면 dongId는 그대로 사용
    return {"dongId": targetDong, "propertyIds": recs}


def hybrid_recommend(user_id: int, top_k=5) -> list:
    """
    하이브리드 추천:
      1) SVD 협업 필터링 기반 추천 (rec_svd)
      2) 콘텐츠 기반 추천(회사/학교 위치 가중치 포함) → content_based_with_office_location 사용 (rec_cb)
      3) 두 결과를 병합(중복 제거)한 후 상위 top_k 반환.
      
    반환 예시: [propertyId1, propertyId2, ...]
    """
    logger.info("Starting hybrid recommendation for user %d...", user_id)
    
    # 1) SVD 협업 추천
    rec_svd = recommend(user_id, top_k)  # 예: [pid1, pid2, ...]

    # 2) 콘텐츠 기반 추천 (회사/학교 위치 고려)
    try:
        from app.modules.content_based.services.content_based_with_office import content_based_with_office_location
    except ImportError:
        logger.error("content_based_with_office_location 모듈을 찾을 수 없습니다.")
        rec_cb = []
    else:
        cb_res = content_based_with_office_location(user_id, top_k)
        # 반환형: {"dongId": <dong_id>, "propertyIds": [pid, ...]}
        rec_cb = cb_res.get("propertyIds", [])

    # 3) 두 추천 결과를 병합 (중복 제거, 순서 유지)
    combined = rec_svd + rec_cb
    merged = list(dict.fromkeys(combined))
    merged = merged[:top_k]
    logger.info("Hybrid recommendation for user %d: %s", user_id, merged)
    return merged



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
