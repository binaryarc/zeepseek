# app/routers/property_score_router.py

from typing import Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from app.modules.property_scoring.normalization_service import normalize_scores_and_update
# 배치 처리 로직 (scoring_batch.py)
from app.modules.property_scoring.scoring_batch import (
    recalculate_all_scores_no_batch,
    recalculate_all_scores_single,
    recalculate_all_scores_batch,
    recalculate_incomplete_scores_batch,
    update_property_score_optimized
)

# 점수 계산 서비스 (scoring_service.py)
from app.modules.property_scoring.scoring_service import compute_property_score

# 추천 로직 (recommend_service.py)
from app.modules.content_based.services.recommend_service import recommend_properties

router = APIRouter()


class NewPropertyData(BaseModel):
    property_id: int
    latitude: float
    longitude: float


@router.post("/calculate", summary="Calculate and update score for a new property")
def calculate_new_property_score(data: NewPropertyData):
    """
    1) compute_property_score(...)를 이용해 점수를 계산합니다.
    2) update_property_score_optimized(...)를 통해 DB에 갱신합니다.
    """
    score_data = compute_property_score(data.dict())
    success = update_property_score_optimized(data.property_id, score_data)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to update property score")
    return {"property_id": data.property_id, "score_data": score_data}


@router.post("/recalculate/no_batch", summary="Recalculate scores for all properties (no batch)")
def recalculate_no_batch():
    """
    전체 매물을 한 번에 로드한 뒤, 단일 스레드로 순차 처리합니다.
    """
    total = recalculate_all_scores_no_batch()
    return {"message": f"Processed {total} properties (no batch)."}


@router.post("/recalculate/single", summary="Recalculate scores for all properties (single-threaded batch)")
def recalculate_single():
    """
    단일 스레드 배치 방식 (예: limit=20000, batch_size=1000 기본값)으로 테스트용 처리합니다.
    """
    total = recalculate_all_scores_single()
    return {"message": f"Processed {total} properties (single-threaded batch)."}


class BatchRecalcParams(BaseModel):
    batch_size: int = 1000
    max_workers: int = 8
    limit: Optional[int] = None  # Python 3.9 이하에서는 Optional[int] 사용


@router.post("/recalculate/batch", summary="Recalculate scores for all properties (multi-threaded batch)")
def recalculate_batch(params: BatchRecalcParams):
    """
    멀티 스레드 배치 방식으로 매물 점수를 재계산합니다.
    """
    total = recalculate_all_scores_batch(
        batch_size=params.batch_size,
        max_workers=params.max_workers,
        limit=params.limit
    )
    return {"message": f"Processed {total} properties (multi-threaded batch)."}


@router.post("/recalculate/incomplete", summary="Recalculate incomplete scores for properties")
def recalculate_incomplete():
    """
    property_score가 없거나 각 카테고리의 count가 0인 매물만 재계산합니다.
    """
    total = recalculate_incomplete_scores_batch()
    return {"message": f"Processed {total} incomplete properties."}


class UserCategoryScore(BaseModel):
    transport_score: float = Field(..., alias="transportScore")
    restaurant_score: float = Field(..., alias="restaurantScore")
    health_score: float = Field(..., alias="healthScore")
    convenience_score: float = Field(..., alias="convenienceScore")
    cafe_score: float = Field(..., alias="cafeScore")
    chicken_score: float = Field(..., alias="chickenScore")
    leisure_score: float = Field(..., alias="leisureScore")

    class Config:
        allow_population_by_field_name = True


@router.post("/recommend", summary="Recommend top 10 properties based on user's category scores")
def recommend_properties_endpoint(user_scores: UserCategoryScore):
    """
    사용자 점수를 받아 코사인 유사도 기반으로 상위 10개 매물을 추천합니다.
    """
    recommendations = recommend_properties(user_scores.dict(), top_n=10)
    if not recommendations:
        raise HTTPException(status_code=404, detail="No properties found")
    return {"recommended_properties": recommendations}



@router.post("/normalize", summary="Normalize all property scores (0~100 scale)")
def normalize_all_property_scores():
    """
    property_score 테이블의 점수를 정규화하여 0~100 범위로 저장합니다.
    """
    normalize_scores_and_update()
    return {"message": "All property scores normalized successfully (0~100 scale)."}
