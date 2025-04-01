from fastapi import APIRouter
from app.modules.ai_recommender.recommend_service import train_model, recommend

router = APIRouter()
model = train_model()  # 서버 시작 시 학습

@router.get("/recommend/{user_id}")
def get_recommend(user_id: int):
    return {"recommendations": recommend(user_id)}
