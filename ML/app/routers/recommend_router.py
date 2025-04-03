# app/routers/recommendation_router.py

from typing import Optional
import logging
from fastapi import APIRouter, HTTPException, Body, Query
from pydantic import BaseModel, Field

# 콘텐츠 기반 추천 서비스 (사용자 점수 기반 추천)
from app.modules.content_based.services.recommend_service import recommend_properties
# AI 추천 관련 서비스: 학습용 함수는 그대로 사용
from app.modules.ai_recommender.recommend_service import train_model
# 수정: AI 추천시 user 테이블 조회 후 추천하는 래퍼 함수 사용
from app.modules.ai_recommender.recommend_service import train_model, recommend_for_mainpage

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

router = APIRouter()

# ==========================
# 1. 사용자 카테고리 점수를 이용한 추천
# ==========================
class UserCategoryScore(BaseModel):
    transport_score: float = Field(..., alias="transportScore")
    restaurant_score: float = Field(..., alias="restaurantScore")
    health_score: float = Field(..., alias="healthScore")
    convenience_score: float = Field(..., alias="convenienceScore")
    cafe_score: float = Field(..., alias="cafeScore")
    chicken_score: float = Field(..., alias="chickenScore")
    leisure_score: float = Field(..., alias="leisureScore")
    gender: Optional[int] = Field(None, alias="gender")
    age: Optional[int] = Field(None, alias="age")
    user_id: Optional[int] = Field(None, alias="userId")
    
    class Config:
        allow_population_by_field_name = True

@router.post("/recommend", summary="Recommend top 10 properties based on user's category scores")
def recommend_properties_endpoint(user_scores: UserCategoryScore):
    """
    사용자 점수를 받아 코사인 유사도 기반으로 상위 10개 매물을 추천합니다.
    """
    try:
        user_data = user_scores.model_dump()
    except AttributeError:
        user_data = user_scores.dict()
    
    recommendations = recommend_properties(
        user_scores=user_data,
        top_n=10,
        gender=user_scores.gender,
        age=user_scores.age
    )
    
    if not recommendations:
        raise HTTPException(status_code=404, detail="No properties found")
    
    # maxType이 존재하면 이를 반환 (없으면 None)
    global_max_type = None
    for rec in recommendations:
        if rec.get("maxType") is not None:
            global_max_type = rec["maxType"]
            break

    return {
        "recommended_properties": recommendations,
        "maxType": global_max_type
    }

# ==========================
# 2. AI 기반 추천 엔드포인트
# ==========================
@router.get("/ai-recommend", summary="AI 기반 추천 (GET)")
def get_ai_recommend(user_id: int = Query(..., description="추천 요청 대상 사용자 ID")):
    """
    GET 방식 AI 추천 엔드포인트.
    - user 테이블에서 사용자 정보를 조회한 후 추천을 수행합니다.
    """
    logger.info("GET AI 추천 요청: user_id=%s", user_id)
    result = get_recommendations_for_user(user_id)
    logger.info("GET AI 추천 결과: %s", result)
    return result

@router.post("/ai-recommend", summary="AI 기반 추천 (POST)")
def post_ai_recommend(data: dict = Body(...)):
    """
    POST 방식 AI 추천 엔드포인트.
    요청 Body 예시:
    {
      "user_id": 123
    }
    """
    logger.info("POST AI 추천 요청 데이터: %s", data)
    user_id = data.get("user_id")
    result = get_recommendations_for_user(user_id)
    logger.info("POST AI 추천 결과: %s", result)
    return result

@router.get("/train", summary="AI 추천 모델 학습")
def train_endpoint():
    """
    /ai-recommend/train 엔드포인트를 호출하면,
    Elasticsearch의 로그 데이터를 이용해 AI 추천 모델(SVD 기반)을 학습합니다.
    """
    logger.info("AI 추천 모델 학습 요청 시작.")
    train_model()
    logger.info("AI 추천 모델 학습 완료.")
    return {"status": "Model trained successfully"}
