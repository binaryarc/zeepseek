import pandas as pd
import time
import numpy as np
from datetime import datetime, timedelta
from sqlalchemy import text
from sklearn.metrics.pairwise import cosine_similarity
from app.config.elasticsearch import get_es_client  # Elasticsearch 클라이언트를 생성하는 함수
from app.modules.ai_recommender.svd_model import RecommenderModel  # 추천 모델(SVD 기반) 클래스
from app.modules.ai_recommender.action_score import ACTION_SCORE  # action 값을 점수(score)로 매핑하는 딕셔너리
from app.config.database import SessionLocal

# Elasticsearch 클라이언트와 추천 모델 인스턴스 초기화
es = get_es_client()
model = RecommenderModel()

def fetch_logs_from_es():
    """
    Elasticsearch에서 최근 30일 간의 로그 데이터를 조회하여, 
    필요한 컬럼(userId, propertyId, action, dongId)을 포함한 DataFrame으로 반환하는 함수.
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
    Elasticsearch에서 로그 데이터를 가져와 추천 모델을 학습시키는 함수.
    학습된 모델 인스턴스를 반환함.
    """
    df = fetch_logs_from_es()
    model.train(df)
    return model

def get_most_frequent_dong_for_user(user_id: int, duration="2h"):
    """
    최근 duration(기본 2시간) 이내, user_id가 남긴 로그 중 action이 
    "search", "view", 또는 "comment"인 로그에서 가장 많이 등장한 dongId를 반환합니다.
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
                    {"term": {"action": "comment"}}
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
    해당 dong에 속한 매물들만 대상으로 SVD 예측을 수행하여 추천 property_id 리스트를 반환합니다.
    만약 dong 로그가 없으면 fallback으로 recommend(user_id)를 호출합니다.
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
    기본 SVD 협업 필터링 추천: 해당 사용자가 이미 본 매물을 제외한 unseen 매물에 대해
    예측 점수를 계산하여 상위 top_k property_id 리스트를 반환합니다.
    """
    df = fetch_logs_from_es()
    seen = df[df['user_id'] == user_id]['property_id'].unique()
    all_items = df['property_id'].unique()
    unseen = [pid for pid in all_items if pid not in seen]
    predictions = [(pid, model.predict(user_id, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)
    return [int(pid) for pid, _ in predictions[:top_k]]

def recommend_for_mainpage(user_id: int, top_k=10, gender=None, age=None):
    """
    메인페이지 추천:
      - 최근 2시간 이내 사용자의 'search', 'view', 'comment' 로그를 확인하여,
        가장 많이 등장한 dongId가 있으면 그 동에 속한 매물들만 대상으로 추천.
      - 없다면 fallback으로 기본 recommend(user_id)를 사용.
    """
    target_dong = get_most_frequent_dong_for_user(user_id, duration="2h")
    if target_dong:
        recs = recommend_by_dong(user_id, top_k=top_k)
        if recs:
            return {"dongId": target_dong, "propertyIds": recs}
    # fallback
    recs = recommend(user_id, top_k=top_k)
    return {"dongId": None, "propertyIds": recs}
