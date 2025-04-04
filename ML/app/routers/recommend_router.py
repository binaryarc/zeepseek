from typing import Optional
import logging
from fastapi import APIRouter, HTTPException, Body, Query
from pydantic import BaseModel
from fastapi.encoders import jsonable_encoder
import numpy as np

# 콘텐츠 기반 추천 서비스 (사용자 점수 기반 추천)
from app.modules.content_based.services.recommend_service import recommend_properties
# AI 추천 관련 서비스: 학습용 함수와 메인페이지 추천 함수 사용
from app.modules.ai_recommender.recommend_service import train_model, recommend_for_mainpage

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

router = APIRouter()

# ==========================
# 1. 사용자 카테고리 점수를 이용한 추천
# ==========================
class UserCategoryScore(BaseModel):
    transportScore: float
    restaurantScore: float
    healthScore: float
    convenienceScore: float
    cafeScore: float
    chickenScore: float
    leisureScore: float
    gender: Optional[int] = None
    age: Optional[int] = None
    userId: Optional[int] = None

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
    
    # 사용자 점수 확인 로깅
    logger.info("[/recommend] UserCategoryScore received: %s", user_data)

    # 실제 추천 호출 전후 로깅
    logger.info("[/recommend] Calling recommend_properties service...")
    recommendations = recommend_properties(
        user_scores=user_data,
        top_n=10,
        gender=user_scores.gender,
        age=user_scores.age
    )
    logger.info("[/recommend] Service returned: %s", recommendations)

    if not recommendations:
        logger.warning("[/recommend] No properties found for given user scores.")
        raise HTTPException(status_code=404, detail="No properties found")

    global_max_type = None
    for rec in recommendations:
        if rec.get("maxType") is not None:
            global_max_type = rec["maxType"]
            break
    
    logger.info("[/recommend] global_max_type: %s", global_max_type)

    response = {
        "recommendedProperties": recommendations,
        "maxType": global_max_type
    }
    logger.info("[/recommend] Final response: %s", response)
    return response


# ==========================
# 2. AI 기반 추천 엔드포인트
# ==========================
@router.get("/ai-recommend", summary="AI 기반 추천 (GET)")
def get_ai_recommend(
    userId: int = Query(..., alias="user_id", description="추천 요청 대상 사용자 ID")
):
    """
    GET 방식 AI 추천 엔드포인트.
    - recommend_for_mainpage 함수를 사용하여 AI 추천을 수행합니다.
    """
    logger.info("[/ai-recommend GET] Received userId=%s", userId)
    
    # 추천 호출
    logger.info("[/ai-recommend GET] Calling recommend_for_mainpage...")
    result = recommend_for_mainpage(userId)
    logger.info("[/ai-recommend GET] Raw result: %s", result)

    # NumPy 타입 -> 파이썬 내장 타입 변환
    result_converted = jsonable_encoder(
        result,
        custom_encoder={
            np.int64: int,
            np.float64: float,
        }
    )
    logger.info("[/ai-recommend GET] Converted result: %s", result_converted)

    return result_converted


@router.post("/ai-recommend", summary="AI 기반 추천 (POST)")
def post_ai_recommend(data: dict = Body(...)):
    """
    POST 방식 AI 추천 엔드포인트.
    요청 Body 예시:
    {
      "user_id": 123
    }
    """
    logger.info("[/ai-recommend POST] Received body: %s", data)
    userId = data.get("user_id")
    logger.info("[/ai-recommend POST] Extracted userId=%s", userId)

    # 추천 호출
    logger.info("[/ai-recommend POST] Calling recommend_for_mainpage...")
    result = recommend_for_mainpage(userId)
    logger.info("[/ai-recommend POST] Raw result: %s", result)

    # NumPy 타입 -> 파이썬 내장 타입 변환
    result_converted = jsonable_encoder(
        result,
        custom_encoder={
            np.int64: int,
            np.float64: float,
        }
    )
    logger.info("[/ai-recommend POST] Converted result: %s", result_converted)

    return result_converted


@router.get("/train", summary="AI 추천 모델 학습")
def train_endpoint():
    """
    /ai-recommend/train 엔드포인트를 호출하면,
    Elasticsearch의 로그 데이터를 이용해 AI 추천 모델(SVD 기반)을 학습합니다.
    """
    logger.info("[/train] AI 추천 모델 학습 요청 시작.")
    train_model()
    logger.info("[/train] AI 추천 모델 학습 완료.")
    return {"status": "Model trained successfully"}
