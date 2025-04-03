import pandas as pd
import time
import numpy as np
from datetime import datetime, timedelta
from sqlalchemy import text
from sklearn.metrics.pairwise import cosine_similarity

from app.config.elasticsearch import get_es_client  # ES 클라이언트를 생성하는 함수
from app.modules.ai_recommender.svd_model import RecommenderModel  # SVD 기반 추천 모델 클래스
from app.modules.ai_recommender.action_score import ACTION_SCORE  # 업데이트된 ACTION_SCORE 매핑 (zzim 포함)
from app.config.database import SessionLocal

# Elasticsearch 클라이언트와 SVD 모델 인스턴스 초기화
es = get_es_client()
model = RecommenderModel()

def fetch_logs_from_es():
    """
    ES에서 최근 30일간의 로그 데이터를 조회하여, 
    필요한 컬럼(userId, propertyId, action, dongId)을 포함한 DataFrame으로 반환.
    """
    query = {
        "query": {
            "range": {
                "time": { "gte": "now-30d/d" }
            }
        },
        "_source": ["userId", "propertyId", "action", "dongId"]
    }
    result = es.search(index="logs", body=query, size=10000, scroll="2m")
    docs = [doc["_source"] for doc in result["hits"]["hits"]]
    df = pd.DataFrame(docs)
    df['score'] = df['action'].map(ACTION_SCORE)
    df.rename(columns={"userId": "user_id", "propertyId": "property_id", "dongId": "dong_id"}, inplace=True)
    return df[['user_id', 'property_id', 'score', 'dong_id']]

def train_model():
    """
    ES에서 로그 데이터를 가져와 추천 모델을 학습시킴.
    학습된 모델 인스턴스를 반환.
    """
    df = fetch_logs_from_es()
    model.train(df)
    return model

def get_most_frequent_dong_for_user(user_id: int, duration="2h"):
    """
    최근 duration(기본 2시간) 이내, user_id가 남긴 로그 중
    action이 "search", "view", "comment", "zzim"인 로그에서 가장 많이 등장한 dongId를 반환.
    """
    query = {
        "query": {
            "bool": {
                "must": [
                    {"term": {"userId": user_id}},
                    {"range": {"time": {"gte": f"now-{duration}"}}}
                ],
                "should": [
                    {"term": {"action": "search"}},
                    {"term": {"action": "view"}},
                    {"term": {"action": "comment"}},
                    {"term": {"action": "zzim"}}
                ],
                "minimum_should_match": 1
            }
        },
        "size": 10000,
        "_source": ["dongId"]
    }
    res = es.search(index="logs", body=query, scroll="2m")
    docs = [doc["_source"] for doc in res["hits"]["hits"] if doc["_source"].get("dongId")]
    if not docs:
        return None
    df = pd.DataFrame(docs)
    most_common = df['dongId'].value_counts().idxmax()
    return most_common

def recommend_by_dong(user_id: int, top_k=10):
    """
    사용자의 최근 2시간 로그에서 가장 많이 등장한 dongId를 추출하고,
    해당 dong에 속한 매물들만 대상으로 SVD 예측을 수행하여 추천 property_id 리스트를 반환.
    dong 로그가 없으면 fallback으로 recommend(user_id)를 호출.
    """
    target_dong = get_most_frequent_dong_for_user(user_id, duration="2h")
    if target_dong is None:
        return recommend(user_id, top_k)
    
    df = fetch_logs_from_es()
    candidates_df = df[df['dong_id'] == target_dong]
    if candidates_df.empty:
        return recommend(user_id, top_k)
    
    seen = df[df['user_id'] == user_id]['property_id'].unique()
    candidate_items = candidates_df['property_id'].unique()
    unseen = [pid for pid in candidate_items if pid not in seen]
    if not unseen:
        unseen = candidate_items

    predictions = [(pid, model.predict(user_id, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)
    recommended = [int(pid) for pid, _ in predictions[:top_k]]
    return recommended

def recommend(user_id: int, top_k=10):
    """
    기본 SVD 협업 필터링 추천:
    해당 사용자가 이미 본 매물을 제외한 unseen 매물에 대해
    예측 점수를 계산하여 상위 top_k property_id 리스트를 반환.
    """
    df = fetch_logs_from_es()
    seen = df[df['user_id'] == user_id]['property_id'].unique()
    all_items = df['property_id'].unique()
    unseen = [pid for pid in all_items if pid not in seen]
    predictions = [(pid, model.predict(user_id, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)
    return [int(pid) for pid, _ in predictions[:top_k]]

def recommend_content_based(user_preferences: dict, top_k=10, dong_id=None):
    """
    콘텐츠 기반 추천:
    - 사용자가 중요하게 생각하는 최대 3개 카테고리 점수를 기반으로 매물의 콘텐츠 점수를 계산.
    - property_score 데이터를 load_property_vectors()를 통해 가져와,
      사용자 선호 벡터와 내적(dot product)하여 점수를 산출.
    - dong_id가 제공되면 해당 dong의 매물만 필터링.
    """
    property_array, property_ids = load_property_vectors()
    if property_array is None:
        return []
    category_names = ["transport", "restaurant", "health", "convenience", "cafe", "chicken", "leisure"]
    user_vector = np.array([user_preferences.get(cat, 0) for cat in category_names])
    
    if dong_id is not None:
        session = SessionLocal()
        try:
            query = text("SELECT property_id FROM property WHERE dong_id = :dong_id")
            rows = session.execute(query, {"dong_id": dong_id}).fetchall()
            valid_ids = set([row[0] for row in rows])
        finally:
            session.close()
        indices = [i for i, pid in enumerate(property_ids) if pid in valid_ids]
        if not indices:
            return []
        property_array = property_array[indices]
        property_ids = [property_ids[i] for i in indices]
    
    scores = property_array.dot(user_vector)
    sorted_indices = np.argsort(scores)[::-1]
    recommended = [int(property_ids[i]) for i in sorted_indices[:top_k]]
    return recommended

def recommend_for_mainpage(user_id: int, top_k=10, gender=None, age=None, user_preferences=None):
    """
    메인페이지 추천:
      1. 최근 2시간 내 사용자의 'search', 'view', 'comment', 'zzim' 로그를 확인하여,
         가장 많이 등장한 dongId가 있으면 해당 dong에 속한 매물들만 대상으로 SVD 추천을 수행.
      2. 로그가 충분하지 않으면, 파라미터로 넘어온 user_preferences(최대 3개 중요 카테고리)를 활용한 콘텐츠 기반 추천 수행.
      3. fallback: 기본 SVD 협업 필터링 추천.
    """
    target_dong = get_most_frequent_dong_for_user(user_id, duration="2h")
    if target_dong:
        recs = recommend_by_dong(user_id, top_k=top_k)
        if recs and len(recs) >= top_k // 2:
            return {"dongId": target_dong, "propertyIds": recs}
    
    if user_preferences is not None:
        recs = recommend_content_based(user_preferences, top_k=top_k, dong_id=target_dong)
        return {"dongId": target_dong, "propertyIds": recs}
    
    recs = recommend(user_id, top_k=top_k)
    return {"dongId": None, "propertyIds": recs}
