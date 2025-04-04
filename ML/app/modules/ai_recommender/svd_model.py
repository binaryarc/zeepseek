import pandas as pd
from surprise import Dataset, Reader, SVD, dump

class RecommenderModel:
    def __init__(self):
        # 아직 학습하기 전이므로 None
        self.model = None
        self.trainset = None

    def is_trained(self) -> bool:
        """
        내부적으로 SVD 모델(self.model)과 trainset이 모두 존재하면 True 반환.
        """
        return (self.model is not None) and (self.model.trainset is not None)

    def train(self, df: pd.DataFrame):
        """
        userId, propertyId, score 컬럼을 가진 df를 가지고 SVD 모델 학습.
        """
        if df.empty:
            raise ValueError("학습할 데이터가 없습니다. (DataFrame이 비어 있음)")

        # Surprise용 Reader: 평점 범위 0 ~ 20
        reader = Reader(rating_scale=(0, 20))
        # df에서 필요한 컬럼만 추출하여 Dataset 생성
        data = Dataset.load_from_df(df[['userId', 'propertyId', 'score']], reader)
        # 전체 데이터를 사용하여 Trainset 생성
        trainset = data.build_full_trainset()

        # SVD 모델 학습
        self.model = SVD()
        self.model.fit(trainset)
        self.trainset = trainset

    def predict(self, user_id: int, property_id: int) -> float:
        """
        학습된 SVD 모델에 기반하여 user_id, property_id에 대한 예측 평점을 반환.
        """
        if not self.is_trained():
            raise ValueError("모델이 아직 초기화되지 않았습니다. 먼저 train()을 호출하세요.")
        prediction = self.model.predict(uid=user_id, iid=property_id)
        return prediction.est

    def save_model(self, file_path: str):
        """
        Surprise의 dump()를 사용하여 모델(및 내부 trainset)을 파일로 저장.
        """
        if not self.is_trained():
            raise ValueError("학습된 모델이 없어 저장할 수 없습니다.")
        dump.dump(file_path, algo=self.model)

    def load_model(self, file_path: str):
        """
        Surprise의 load()를 사용하여 모델을 파일에서 로드.
        """
        _, loaded_algo = dump.load(file_path)
        self.model = loaded_algo
        self.trainset = loaded_algo.trainset
        # 로드 후에는 is_trained()가 True가 되므로 바로 predict()를 호출할 수 있음
