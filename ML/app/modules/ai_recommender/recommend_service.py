import pandas as pd
from config.elasticsearch import get_es_client
from app.modules.ai_recommender.svd_model import RecommenderModel
from app.modules.ai_recommender.action_score import ACTION_SCORE

es = get_es_client()
model = RecommenderModel()

def fetch_logs_from_es():
    query = {
        "query": {
            "range": {
                "time": { "gte": "now-30d/d" }
            }
        },
        "_source": ["userId", "id", "action"]
    }

    result = es.search(index="logs", body=query, size=10000, scroll="2m")
    docs = [doc["_source"] for doc in result["hits"]["hits"]]
    df = pd.DataFrame(docs)

    df['score'] = df['action'].map(ACTION_SCORE)
    df.rename(columns={"userId": "user_id", "id": "property_id"}, inplace=True)
    return df[['user_id', 'property_id', 'score']]

def train_model():
    df = fetch_logs_from_es()
    model.train(df)
    return model

def recommend(user_id: int, top_k=5):
    df = fetch_logs_from_es()
    seen = df[df['user_id'] == user_id]['property_id'].unique()
    all_items = df['property_id'].unique()
    unseen = [pid for pid in all_items if pid not in seen]

    predictions = [(pid, model.predict(user_id, pid)) for pid in unseen]
    predictions.sort(key=lambda x: x[1], reverse=True)
    return [int(pid) for pid, _ in predictions[:top_k]]
