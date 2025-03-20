from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.scoring_service import compute_property_score, update_property_score, recalculate_all_scores

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

@router.post("/recalculate", summary="Recalculate scores for all properties")
def recalculate_scores():
    count = recalculate_all_scores()
    return {"message": f"Recalculated scores for {count} properties."}
