import pandas as pd  # pandas 라이브러리: 데이터프레임 조작을 위해 사용
from surprise import Dataset, Reader, SVD  # 추천 시스템 구축에 필요한 클래스들 임포트

class RecommenderModel:
    def __init__(self):
        # SVD 알고리즘을 사용하여 추천 모델 객체를 초기화합니다.
        self.model = SVD()

    def train(self, df: pd.DataFrame):
        # 평점 데이터의 범위를 0에서 20으로 지정하는 Reader 객체 생성
        reader = Reader(rating_scale=(0, 20))
        # 데이터프레임에서 'user_id', 'property_id', 'score' 컬럼만 추출하여 Surprise 데이터셋으로 변환
        data = Dataset.load_from_df(df[['user_id', 'property_id', 'score']], reader)
        # 전체 데이터를 사용하여 학습용 데이터셋 생성 (trainset)
        trainset = data.build_full_trainset()
        # SVD 모델을 학습시킵니다.
        self.model.fit(trainset)

    def predict(self, user_id: int, property_id: int) -> float:
        # 주어진 user_id와 property_id에 대해 예측된 평점(estimation)을 반환합니다.
        return self.model.predict(user_id, property_id).est
