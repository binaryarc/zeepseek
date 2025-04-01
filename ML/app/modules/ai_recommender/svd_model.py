import pandas as pd
from surprise import Dataset, Reader, SVD

class RecommenderModel:
    def __init__(self):
        self.model = SVD()

    def train(self, df: pd.DataFrame):
        reader = Reader(rating_scale=(0, 20))
        data = Dataset.load_from_df(df[['user_id', 'property_id', 'score']], reader)
        trainset = data.build_full_trainset()
        self.model.fit(trainset)

    def predict(self, user_id: int, property_id: int) -> float:
        return self.model.predict(user_id, property_id).est
