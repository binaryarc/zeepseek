# app/routers/property_score_router.py

from typing import Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

# 배치 처리 로직이 scoring_batch.py 라는 파일에 있다고 가정
from app.services.scoring_batch import (
    recalculate_all_scores_no_batch,
    recalculate_all_scores_single,
    recalculate_all_scores_batch,
    recalculate_incomplete_scores_batch,
    update_property_score_optimized  # 만약 이 함수도 scoring_batch 쪽에 있다면 이렇게 임포트
)

# 점수 계산은 scoring_service에서 한다면 (예: compute_property_score)
from app.services.scoring_service import compute_property_score

# 추천 로직은 recommend_service에서
from app.services.recommend_service import recommend_properties

router = APIRouter()


class NewPropertyData(BaseModel):
    property_id: int
    latitude: float
    longitude: float


@router.post("/calculate", summary="Calculate and update score for a new property")
def calculate_new_property_score(data: NewPropertyData):
    """
    1) compute_property_score(...)로 점수 계산
    2) update_property_score_optimized(...)로 DB에 갱신
    """
    score_data = compute_property_score(data.dict())
    success = update_property_score_optimized(data.property_id, score_data)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to update property score")
    return {"property_id": data.property_id, "score_data": score_data}


@router.post("/recalculate/no_batch", summary="Recalculate scores for all properties (no batch)")
def recalculate_no_batch():
    """
    전체 매물을 한 번에 로드한 뒤, 단일 스레드로 순차 처리.
    """
    total = recalculate_all_scores_no_batch()
    return {"message": f"Processed {total} properties (no batch)."}


@router.post("/recalculate/single", summary="Recalculate scores for all properties (single-threaded batch)")
def recalculate_single():
    """
    단일 스레드 배치 방식. 기본값(예: limit=20000, batch_size=1000)으로 테스트용 처리
    """
    total = recalculate_all_scores_single()
    return {"message": f"Processed {total} properties (single-threaded batch)."}


class BatchRecalcParams(BaseModel):
    batch_size: int = 1000
    max_workers: int = 8
    limit: Optional[int] = None  # Python 3.9 이하에서는 Optional[int]


@router.post("/recalculate/batch", summary="Recalculate scores for all properties (multi-threaded batch)")
def recalculate_batch(params: BatchRecalcParams):
    """
    멀티 스레드 배치 방식.
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
    property_score가 없거나, 각 카테고리 count가 0인 매물만 재계산
    """
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
    """
    사용자 점수를 받아, 코사인 유사도 기반으로 상위 5개 매물을 추천
    """
    recommendations = recommend_properties(user_scores.dict(), top_n=10)
    if not recommendations:
        raise HTTPException(status_code=404, detail="No properties found")
    return {"recommended_properties": recommendations}
