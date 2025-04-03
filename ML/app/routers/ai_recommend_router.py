from fastapi import APIRouter, Body, Query
import logging
from app.modules.ai_recommender.recommend_service import (
    train_model,
    recommend_for_mainpage
)

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

router = APIRouter()

@router.get("/ai-recommend", summary="AI 기반 추천 (GET)")
def get_ai_recommend(user_id: int = Query(..., description="추천 요청 대상 사용자 ID")):
    """
    GET 방식 AI 추천 엔드포인트.
    - 전달받은 user_id만으로 추천 서비스를 호출합니다.
    - 나머지 정보는 내부에서 DB 조회 등을 통해 결정합니다.
    """
    logger.info("GET AI 추천 요청: user_id=%s", user_id)
    result = recommend_for_mainpage(user_id)
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
    다른 추천 관련 파라미터는 내부에서 조회합니다.
    """
    logger.info("POST AI 추천 요청 데이터: %s", data)
    user_id = data.get("user_id")
    result = recommend_for_mainpage(user_id)
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
