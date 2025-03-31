# app/routers/property_score_router.py

from typing import Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

# 배치 처리 및 점수 계산 관련 기존 라우트는 그대로 유지
from app.services.scoring_batch import (
    recalculate_all_scores_no_batch,
    recalculate_all_scores_single,
    recalculate_all_scores_batch,
    recalculate_incomplete_scores_batch,
    update_property_score_optimized
)

# 점수 계산 서비스 (scoring_service.py)
from app.services.scoring_service import compute_property_score

# 추천 로직: AI 적용 버전
from app.services.recommend_service_with_ai import recommend_properties

router = APIRouter()

class NewPropertyData(BaseModel):
    property_id: int
    latitude: float
    longitude: float

@router.post("/calculate", summary="Calculate and update score for a new property")
def calculate_new_property_score(data: NewPropertyData):
    score_data = compute_property_score(data.dict())
    success = update_property_score_optimized(data.property_id, score_data)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to update property score")
    return {"property_id": data.property_id, "score_data": score_data}

@router.post("/recalculate/no_batch", summary="Recalculate scores for all properties (no batch)")
def recalculate_no_batch():
    total = recalculate_all_scores_no_batch()
    return {"message": f"Processed {total} properties (no batch)."}

@router.post("/recalculate/single", summary="Recalculate scores for all properties (single-threaded batch)")
def recalculate_single():
    total = recalculate_all_scores_single()
    return {"message": f"Processed {total} properties (single-threaded batch)."}

class BatchRecalcParams(BaseModel):
    batch_size: int = 1000
    max_workers: int = 8
    limit: Optional[int] = None

@router.post("/recalculate/batch", summary="Recalculate scores for all properties (multi-threaded batch)")
def recalculate_batch(params: BatchRecalcParams):
    total = recalculate_all_scores_batch(
        batch_size=params.batch_size,
        max_workers=params.max_workers,
        limit=params.limit
    )
    return {"message": f"Processed {total} properties (multi-threaded batch)."}

@router.post("/recalculate/incomplete", summary="Recalculate incomplete scores for properties")
def recalculate_incomplete():
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
def recommend_properties_endpoint(user: UserCategoryScore):
    recommendations = recommend_properties(user_scores=user.dict(), top_n=10)
    if not recommendations:
        raise HTTPException(status_code=404, detail="No properties found")
    return {"recommended_properties": recommendations}
