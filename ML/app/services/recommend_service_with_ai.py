import numpy as np
import logging
from sqlalchemy.orm import Session
from sklearn.metrics.pairwise import cosine_similarity
from tensorflow.keras.models import load_model

from app.config.database import SessionLocal
from app.utils.caching import (
    get_category_mean_std_values,
    get_category_min_max_values,
    load_property_vectors
)
from app.services.mlp_model import train_mlp_model
# from app.services.ann_index import build_annoy_index, query_annoy_index  # 필요 시 주석 해제

logger = logging.getLogger(__name__)

def apply_mmr(similarities, property_vectors, top_n, diversity_lambda=0.5):
    selected = []
    candidate_indices = list(range(len(similarities)))
    first = np.argmax(similarities)
    selected.append(first)
    candidate_indices.remove(first)
    
    while len(selected) < top_n and candidate_indices:
        mmr_scores = []
        for i in candidate_indices:
            sim_selected = max(cosine_similarity(property_vectors[i].reshape(1, -1),
                                                   property_vectors[selected]).flatten())
            score = diversity_lambda * similarities[i] - (1 - diversity_lambda) * sim_selected
            mmr_scores.append((i, score))
        best_candidate, _ = max(mmr_scores, key=lambda x: x[1])
        selected.append(best_candidate)
        candidate_indices.remove(best_candidate)
    
    return selected

def recommend_properties(
    user_scores: dict, 
    top_n=5, 
    apply_mmr_flag=True, 
    diversity_lambda=0.5, 
    normalization_method='minmax',
    ai_method='mlp',  # 'mlp', 'collaborative', 'lambdamart'
    use_ann=False, 
    ann_top_k=1000
):
    """
    :param user_scores: ex) {"transport_score":0.5, "restaurant_score":0.5, ...}
    :param ai_method: 'mlp'|'collaborative'|'lambdamart' 등 확장성
    :param use_ann: True면 ANN 사용
    :return: 추천된 매물 목록
    """
    # 1. 캐싱된 매물 벡터 로드
    property_array, property_ids = load_property_vectors()
    if property_array is None:
        logger.info("No property scores found.")
        return []
    logger.info("Fetched %d properties from cache.", property_array.shape[0])
    
    # 2. 정규화 파라미터 준비
    keys = [
        "transport_score", "restaurant_score", "health_score", 
        "convenience_score", "cafe_score", "chicken_score", "leisure_score"
    ]
    
    if normalization_method == 'minmax':
        min_max_values = get_category_min_max_values()
        mins = np.array([min_max_values[key][0] for key in keys])
        maxs = np.array([min_max_values[key][1] for key in keys])
        denom = maxs - mins
        denom[denom == 0] = 1
        norm_array = (property_array - mins) / denom
        
        user_vals = np.array([user_scores.get(k, 0) for k in keys])
        user_vector = user_vals.reshape(1, -1)
        logger.info("Using min-max normalization.")
    else:
        mean_std_values = get_category_mean_std_values()
        means = np.array([mean_std_values[key][0] for key in keys])
        stds = np.array([mean_std_values[key][1] for key in keys])
        stds[stds == 0] = 1
        norm_array = (property_array - means) / stds
        
        user_vals = np.array([user_scores.get(k, 0) for k in keys])
        user_vector = ((user_vals - means) / stds).reshape(1, -1)
        logger.info("Using z-score normalization.")
    
    # 3. 가중치 (예: 고정값)
    category_weights = np.array([1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.7])
    norm_array = norm_array * category_weights
    user_vector = user_vector * category_weights
    
    # 4. 후보군 필터링
    if use_ann:
        # from app.services.ann_index import build_annoy_index, query_annoy_index
        # annoy_index = build_annoy_index(norm_array, num_trees=10)
        # candidate_order = query_annoy_index(annoy_index, user_vector, top_k=ann_top_k)
        # candidate_vectors = norm_array[candidate_order]
        # candidate_similarities = cosine_similarity(candidate_vectors, user_vector).flatten()
        # logger.info("ANN selected %d candidate properties.", len(candidate_order))
        logger.info("ANN not implemented in this minimal example. Using full search.")
        use_ann = False  # 강제 off
        # (필요 시 위 주석 해제 후 사용)
    
    if not use_ann:
        full_similarities = cosine_similarity(norm_array, user_vector).flatten()
        top_k = min(ann_top_k, len(full_similarities))
        candidate_order = np.argsort(full_similarities)[-top_k:].tolist()
        candidate_vectors = norm_array[candidate_order]
        candidate_similarities = full_similarities[candidate_order]
        logger.info("Selected top %d candidate properties from full search.", top_k)
    
    # 5. AI 모델 적용
    if ai_method == 'mlp':
        from tensorflow.keras.models import load_model
        try:
            model = load_model('mlp_model.h5')
            logger.info("MLP model loaded from file.")
        except Exception as e:
            logger.error("MLP model not found. Training new model. Error: %s", e)
            model = train_mlp_model()
        
        # 후보군에 대해 MLP 점수 예측
        mlp_scores = model.predict([
            user_vector.repeat(candidate_vectors.shape[0], axis=0), 
            candidate_vectors
        ]).flatten()
        
        # 간단히 기존 유사도와 앙상블
        candidate_similarities = (candidate_similarities + mlp_scores) / 2.0
        logger.info("MLP-based adjustment applied.")
    
    elif ai_method == 'collaborative':
        # 협업 필터링 로직 (필요 시 구현)
        logger.info("Collaborative filtering-based adjustment (not implemented).")
    
    elif ai_method == 'lambdamart':
        # LambdaMART 로직 (필요 시 구현)
        logger.info("LambdaMART-based adjustment (not implemented).")
    
    else:
        logger.info("No additional AI adjustment applied.")
    
    # 6. MMR 후처리
    if apply_mmr_flag:
        selected_candidate_indices = apply_mmr(candidate_similarities, candidate_vectors, top_n, diversity_lambda)
        final_selected_indices = [candidate_order[i] for i in selected_candidate_indices]
        top_properties = [{
            "propertyId": property_ids[i], 
            "similarity": float(candidate_similarities[candidate_order.index(i)])
        } for i in final_selected_indices]
    else:
        properties_rec = [{
            "propertyId": property_ids[i], 
            "similarity": float(candidate_similarities[candidate_order.index(i)])
        } for i in candidate_order]
        top_properties = sorted(properties_rec, key=lambda x: x["similarity"], reverse=True)[:top_n]
    
    return top_properties
