import pandas as pd
from surprise import Dataset, Reader, SVD, dump

class RecommenderModel:
    def __init__(self):
        # SVD 알고리즘을 사용하여 추천 모델 객체를 초기화합니다.
        # (아직 trainset이 없으므로, 바로 predict() 하면 에러가 납니다)
        self.model = None
        self.trainset = None

    def train(self, df: pd.DataFrame):
        """
        주어진 DataFrame(df)을 이용해 모델을 학습합니다.
        df는 'user_id', 'property_id', 'score' 컬럼을 가져야 하며,
        평점(score)은 0 ~ 20 범위라 가정합니다.
        """
        reader = Reader(rating_scale=(0, 20))
        data = Dataset.load_from_df(df[['user_id', 'property_id', 'score']], reader)
        
        # 전체 데이터를 사용하여 학습용 데이터셋(Trainset) 생성
        trainset = data.build_full_trainset()
        
        # SVD 모델 초기화 후 훈련
        self.model = SVD()
        self.model.fit(trainset)
        
        # 필요 시, trainset 자체를 저장(추후 참조가 필요하면)
        self.trainset = trainset

    def predict(self, user_id: int, property_id: int) -> float:
        """
        주어진 user_id, property_id에 대한 예측 평점을 반환합니다.
        단, 모델이 아직 학습되지 않았다면(즉, self.model이 None이거나 trainset이 없음),
        예외를 발생시킵니다.
        """
        if self.model is None:
            raise ValueError("모델이 아직 초기화되지 않았습니다. 먼저 train()을 호출하세요.")
        
        if not hasattr(self.model, 'trainset') or self.model.trainset is None:
            raise ValueError("모델 내부에 trainset이 없습니다. train()이 제대로 수행되었는지 확인하세요.")
        
        # Surprise 알고리즘의 predict() 사용
        prediction = self.model.predict(uid=user_id, iid=property_id)
        return prediction.est

    def save_model(self, file_path: str):
        """
        Surprise 라이브러리에서 권장하는 dump() 함수를 사용하여
        훈련된 모델(algo)을 파일로 저장합니다.
        """
        if self.model is None:
            raise ValueError("아직 학습된 모델이 없으므로 저장할 수 없습니다.")
        
        dump.dump(file_path, algo=self.model)
        print(f"모델이 '{file_path}' 파일로 저장되었습니다.")

    def load_model(self, file_path: str):
        """
        Surprise 라이브러리에서 권장하는 load() 함수를 사용하여
        저장된 모델을 로드합니다.
        """
        _, loaded_algo = dump.load(file_path)
        self.model = loaded_algo
        print(f"모델이 '{file_path}' 파일에서 로드되었습니다.")
