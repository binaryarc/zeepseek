import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Model, load_model
from tensorflow.keras.layers import Input, Dense, Concatenate, Dropout
from tensorflow.keras.optimizers import Adam
import logging

logger = logging.getLogger(__name__)

def create_recommender_model(num_user_features, num_item_features):
    # 사용자 입력 처리
    user_input = Input(shape=(num_user_features,), name='user_input')
    user_dense = Dense(64, activation='relu')(user_input)
    user_dense = Dropout(0.2)(user_dense)
    user_dense = Dense(32, activation='relu')(user_dense)
    
    # 매물 입력 처리
    item_input = Input(shape=(num_item_features,), name='item_input')
    item_dense = Dense(64, activation='relu')(item_input)
    item_dense = Dropout(0.2)(item_dense)
    item_dense = Dense(32, activation='relu')(item_dense)
    
    # 두 입력 결합
    combined = Concatenate()([user_dense, item_dense])
    hidden = Dense(128, activation='relu')(combined)
    hidden = Dropout(0.3)(hidden)
    hidden = Dense(64, activation='relu')(hidden)
    
    # 최종 출력: 추천 점수 (회귀)
    output = Dense(1, activation='linear', name='score')(hidden)
    
    model = Model(inputs=[user_input, item_input], outputs=output)
    model.compile(optimizer=Adam(learning_rate=0.001), loss='mean_squared_error')
    return model

def train_mlp_model():
    # 더미 데이터를 이용한 학습 예제
    num_samples = 1000
    num_user_features = 7
    num_item_features = 7
    user_data = np.random.rand(num_samples, num_user_features)
    item_data = np.random.rand(num_samples, num_item_features)
    targets = np.random.rand(num_samples, 1)
    
    model = create_recommender_model(num_user_features, num_item_features)
    model.fit([user_data, item_data], targets, epochs=10, batch_size=32)
    
    # 모델 저장
    model.save('mlp_model.h5')
    logger.info("MLP model trained and saved to mlp_model.h5")
    return model
