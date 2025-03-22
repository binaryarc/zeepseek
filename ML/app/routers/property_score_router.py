from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.scoring_service import (
    compute_property_score,
    update_property_score_optimized,
    recalculate_all_scores_no_batch,
    recalculate_all_scores_single,
    recalculate_all_scores_batch,
    recalculate_incomplete_scores_batch
)
from app.services.recommend_service import recommend_properties

router = APIRouter()

class NewPropertyData(BaseModel):
    property_id: int
    latitude: float
    longitude: float

@router.post("/calculate", summary="Calculate and update score for a new property")
def calculate_new_property_score(data: NewPropertyData):
    score_data = compute_property_score(data.dict())
    if not update_property_score_optimized(data.property_id, score_data):
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
    limit: int = None  # None이면 전체 데이터를 처리

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
    transport_score: float
    restaurant_score: float
    health_score: float
    convenience_score: float
    cafe_score: float
    chicken_score: float
    leisure_score: float

@router.post("/recommend", summary="Recommend top 5 properties based on user's category scores")
def recommend_properties_endpoint(user_scores: UserCategoryScore):
    recommendations = recommend_properties(user_scores.dict(), top_n=5)
    if not recommendations:
        raise HTTPException(status_code=404, detail="No properties found")
    return {"recommended_properties": recommendations}
