from fastapi import APIRouter, Body, Query
from app.modules.ai_recommender.recommend_service import train_model, recommend_for_mainpage

router = APIRouter()

@router.get("/recommend/{user_id}")
def get_recommend(
    user_id: int,
    top_k: int = Query(10, description="추천할 매물 수"),
    gender: str = Query(None, description="사용자 성별 (예: 'male' 또는 'female')"),
    age: int = Query(None, description="사용자 나이"),
):
    """
    GET 방식 추천 엔드포인트.
    - 최근 2시간 내 사용자의 'search', 'view', 'comment', 'zzim' 로그를 확인하여 해당 동에 속한 매물 추천
      (충분한 로그가 없으면 콘텐츠 기반 또는 기본 SVD 추천으로 fallback)
    """
    return recommend_for_mainpage(user_id, top_k=top_k, gender=gender, age=age, user_preferences=None)

# @router.post("/recommend")
# def get_recommend_post(data: dict = Body(...)):
#     """
#     POST 방식 추천 엔드포인트.
#     요청 Body에 다음과 같은 형식으로 입력:
#     {
#       "user_id": 123,
#       "top_k": 10,
#       "gender": "male",
#       "age": 30,
#       "user_preferences": {
#          "restaurant": 0.8,
#          "cafe": 0.9,
#          "transport": 0.7
#       }
#     }
#     user_preferences 값이 제공되면 콘텐츠 기반 추천을 사용합니다.
#     """
#     user_id = data.get("user_id")
#     top_k = data.get("top_k", 10)
#     gender = data.get("gender")
#     age = data.get("age")
#     user_preferences = data.get("user_preferences")
#     return recommend_for_mainpage(user_id, top_k=top_k, gender=gender, age=age, user_preferences=user_preferences)

@router.get("/train")
def train_endpoint():
    """
    /train 엔드포인트를 호출하면 Elasticsearch의 로그 데이터를 이용해 모델을 학습합니다.
    """
    train_model()  # 모델 학습
    return {"status": "Model trained successfully"}
