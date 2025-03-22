from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.scoring_service import (
    compute_property_score,
    update_property_score,
    recalculate_all_scores_no_batch,
    recalculate_all_scores_single,
    recalculate_all_scores_batch
)
from app.services.recommend_service import recommend_properties  # 추천 함수는 별도 모듈에서 임포트

router = APIRouter()

class NewPropertyData(BaseModel):
    property_id: int
    latitude: float
    longitude: float

@router.post("/calculate", summary="Calculate and update score for a new property")
def calculate_new_property_score(data: NewPropertyData):
    score_data = compute_property_score(data.dict())
    if not update_property_score(data.property_id, score_data):
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

@router.post("/recalculate/batch", summary="Recalculate scores for all properties (multi-threaded batch)")
def recalculate_batch():
    total = recalculate_all_scores_batch()
    return {"message": f"Processed {total} properties (multi-threaded batch)."}

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
