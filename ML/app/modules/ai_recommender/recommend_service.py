import pandas as pd
from app.config.elasticsearch import get_es_client  # Elasticsearch 클라이언트를 생성하는 함수
from app.modules.ai_recommender.svd_model import RecommenderModel  # 추천 모델(SVD 기반) 클래스
from app.modules.ai_recommender.action_score import ACTION_SCORE  # action 값을 점수(score)로 매핑하는 딕셔너리

# Elasticsearch 클라이언트와 추천 모델 인스턴스 초기화
es = get_es_client()
model = RecommenderModel()

def fetch_logs_from_es():
    """
    Elasticsearch에서 최근 30일 간의 로그 데이터를 조회하여, 
    필요한 컬럼(userId, id, action)을 포함한 DataFrame으로 반환하는 함수.
    """
    # Elasticsearch 쿼리: 최근 30일 이내의 데이터 검색, 필요한 컬럼만 반환
    query = {
        "query": {
            "range": {
                "time": { "gte": "now-30d/d" }  # 시간 범위 지정: 현재로부터 30일 전부터 오늘까지
            }
        },
        "_source": ["userId", "propertyId", "action"]  # 반환할 필드 지정
    }

    # Elasticsearch에서 최대 10000건의 로그를 스크롤 방식으로 검색 (scroll은 대용량 데이터 조회에 유용)
    result = es.search(index="logs", body=query, size=10000, scroll="2m")
    
    # 검색 결과에서 실제 데이터 부분(_source)만 추출
    docs = [doc["_source"] for doc in result["hits"]["hits"]]
    # 추출한 데이터를 DataFrame으로 변환
    df = pd.DataFrame(docs)

    # action 값을 ACTION_SCORE 매핑 딕셔너리를 통해 score 컬럼으로 변환
    df['score'] = df['action'].map(ACTION_SCORE)
    # 컬럼명 수정: Elasticsearch 필드명과 모델 학습에 맞게 변경 (userId -> user_id, id -> property_id)
    df.rename(columns={"userId": "user_id", "propertyId": "property_id"}, inplace=True)
    # 모델 학습에 필요한 컬럼만 선택하여 반환
    return df[['user_id', 'property_id', 'score']]

def train_model():
    """
    Elasticsearch에서 로그 데이터를 가져와 추천 모델을 학습시키는 함수.
    학습된 모델 인스턴스를 반환함.
    """
    # Elasticsearch에서 로그 데이터 DataFrame을 가져옴
    df = fetch_logs_from_es()
    # 가져온 데이터를 이용하여 추천 모델을 학습시킴
    model.train(df)
    # 학습된 모델 반환
    return model

def recommend(user_id: int, top_k=10):
    """
    주어진 사용자(user_id)에 대해 추천할 property_id 리스트를 생성하는 함수.
    
    - 먼저 해당 사용자가 이미 본(property_id) 항목을 확인하고,
    - 전체 항목에서 미본(unseen) 항목을 찾은 후,
    - 미본 항목 각각에 대해 예측 점수를 계산하여 상위 top_k개를 추천.
    
    반환값은 추천 property_id의 정수 리스트.
    """
    # Elasticsearch에서 로그 데이터를 가져옴 (추천 점수를 계산하기 위한 최신 데이터)
    df = fetch_logs_from_es()
    
    # 해당 사용자가 이미 상호작용한 property_id 목록 추출
    seen = df[df['user_id'] == user_id]['property_id'].unique()
    # 전체 데이터에서 고유한 property_id 목록 추출
    all_items = df['property_id'].unique()
    # 사용자가 아직 보지 않은(unseen) property_id 목록 생성
    unseen = [pid for pid in all_items if pid not in seen]

    # 미본 항목 각각에 대해 추천 모델로 예측 점수를 계산하여 (property_id, 점수) 튜플 리스트 생성
    predictions = [(pid, model.predict(user_id, pid)) for pid in unseen]
    # 예측 점수를 기준으로 내림차순 정렬하여 높은 점수를 가진 항목이 앞쪽에 오도록 함
    predictions.sort(key=lambda x: x[1], reverse=True)
    # 상위 top_k개의 property_id를 정수형 리스트로 반환
    return [int(pid) for pid, _ in predictions[:top_k]]
