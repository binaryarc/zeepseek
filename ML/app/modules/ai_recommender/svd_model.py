import pandas as pd
from surprise import Dataset, Reader, SVD, dump

class RecommenderModel:
    def __init__(self):
        self.model = None
        self.trainset = None

    def is_trained(self) -> bool:
        """
        내부적으로 SVD 모델(self.model)과 trainset이 모두 존재하면 True.
        """
        return (self.model is not None) and (self.model.trainset is not None)

    def train(self, df: pd.DataFrame):
        """
        df에는 최소한 'userId', 'propertyId', 'score' 컬럼이 있어야 함.
        """
        if df.empty:
            raise ValueError("학습할 데이터가 없습니다. (DataFrame이 비어 있음)")

        # Surprise용 Reader
        reader = Reader(rating_scale=(0, 20))
        # DataFrame → Surprise Dataset
        data = Dataset.load_from_df(df[['userId', 'propertyId', 'score']], reader)

        # 전체 데이터를 사용한 Trainset
        trainset = data.build_full_trainset()

        # SVD 모델 훈련
        self.model = SVD()
        self.model.fit(trainset)
        self.trainset = trainset

    def predict(self, user_id: int, property_id: int) -> float:
        """
        학습된 SVD 모델에 기반하여 예측 평점을 반환.
        """
        if not self.is_trained():
            raise ValueError("모델이 아직 초기화되지 않았습니다. 먼저 train()을 호출하세요.")
        prediction = self.model.predict(uid=user_id, iid=property_id)
        return prediction.est

    def save_model(self, file_path: str):
        """
        학습된 모델을 Surprise의 dump()로 파일에 저장 (trainset 포함).
        """
        if not self.is_trained():
            raise ValueError("학습된 모델이 없어 저장할 수 없습니다.")
        dump.dump(file_path, algo=self.model)

    def load_model(self, file_path: str):
        """
        저장된 모델을 Surprise의 load()로 로드 (trainset 포함).
        """
        _, loaded_algo = dump.load(file_path)
        self.model = loaded_algo
        self.trainset = loaded_algo.trainset
