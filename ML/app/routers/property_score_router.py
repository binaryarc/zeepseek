from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.scoring_service import compute_property_score, update_property_score, recalculate_all_scores, recommend_properties

router = APIRouter()

class NewPropertyData(BaseModel):
    property_id: int
    latitude: float
    longitude: float

"""
curl -X POST "http://localhost:8000/calculate" \
     -H "Content-Type: application/json" \
     -d '{
           "property_id": 1,
           "latitude": 37.55,
           "longitude": 126.97
         }'
"""
@router.post("/calculate", summary="Calculate and update score for a new property")
def calculate_new_property_score(data: NewPropertyData):
    score_data = compute_property_score(data.dict())
    if not update_property_score(data.property_id, score_data):
        raise HTTPException(status_code=500, detail="Failed to update property score")
    return {"property_id": data.property_id, "score_data": score_data}

"""
curl -X POST "http://localhost:8000/recalculate"
"""


@router.post("/recalculate", summary="Recalculate scores for all properties")
def recalculate_scores():
    count = recalculate_all_scores()
    return {"message": f"Recalculated scores for {count} properties."}

class UserCategoryScore(BaseModel):
    transport_score: float
    restaurant_score: float
    health_score: float
    convenience_score: float
    cafe_score: float
    chicken_score: float
    leisure_score: float


"""
curl -X POST "http://localhost:8000/recommend" \
     -H "Content-Type: application/json" \
     -d '{
           "transport_score": 1.0,
           "restaurant_score": 1.0,
           "health_score": 1.0,
           "convenience_score": 1.0,
           "cafe_score": 1.0,
           "chicken_score": 1.0,
           "leisure_score": 1.0
         }'
"""
@router.post("/recommend", summary="Recommend top 5 properties based on user's category scores")
def recommend_properties_endpoint(user_scores: UserCategoryScore):
    recommendations = recommend_properties(user_scores.dict(), top_n=5)
    if not recommendations:
        raise HTTPException(status_code=404, detail="No properties found")
    return {"recommended_properties": recommendations}
