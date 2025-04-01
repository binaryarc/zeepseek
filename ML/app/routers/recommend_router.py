from fastapi import APIRouter
from app.modules.ai_recommender.recommend_service import train_model, recommend

router = APIRouter()

@router.get("/recommend/{user_id}")
def get_recommend(user_id: int):
    return {"recommendations": recommend(user_id)}


@router.get("/train")
def train_endpoint():
    """
    /train 엔드포인트를 호출하면 Elasticsearch의 로그 데이터를 이용해 모델을 학습합니다.
    """
    train_model()  # API 요청 시 모델을 학습시킵니다.
    return {"status": "Model trained successfully"}