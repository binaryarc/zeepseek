import math
import numpy as np
from sqlalchemy import text
from sklearn.metrics.pairwise import cosine_similarity

# haversine 함수 import
from app.utils.haversine import haversine  # lat1, lon1, lat2, lon2 => float(km)
from app.config.database import SessionLocal
import logging

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
if not logger.handlers:
    handler = logging.StreamHandler()
    formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    handler.setFormatter(formatter)
    logger.addHandler(handler)


def content_based_with_office_location(user_id: int, top_k=5):
    logger.info("=== content_based_with_office_location 시작: user_id=%d, top_k=%d ===", user_id, top_k)
    
    # 1) 사용자 정보 로드
    session = SessionLocal()
    try:
        user_sql = text("""
            SELECT dong_id,
                   safe, leisure, restaurant, health,
                   convenience, transport, cafe,
                   latitude AS office_lat, longitude AS office_lon
            FROM user_preference
            WHERE user_id = :uid
        """)
        user_row = session.execute(user_sql, {"uid": user_id}).fetchone()
        if not user_row:
            logger.warning("user_preference 정보가 없습니다. user_id=%d", user_id)
            return {"dongId": None, "propertyIds": []}

        dong_id = user_row._mapping["dong_id"]
        logger.info("사용자 dong_id: %s", dong_id)
        user_cat = [
            float(user_row._mapping["safe"] or 0),
            float(user_row._mapping["leisure"] or 0),
            float(user_row._mapping["restaurant"] or 0),
            float(user_row._mapping["health"] or 0),
            float(user_row._mapping["convenience"] or 0),
            float(user_row._mapping["transport"] or 0),
            float(user_row._mapping["cafe"] or 0)
        ]
        logger.info("사용자 카테고리 점수: %s", user_cat)
        office_lat = float(user_row._mapping["office_lat"] or 0)
        office_lon = float(user_row._mapping["office_lon"] or 0)
        logger.info("사용자 office 위치: (%.6f, %.6f)", office_lat, office_lon)
    finally:
        session.close()

    # 사용자 벡터 (8차원: 7개 카테고리 + 거리 점수 초기 0.0)
    user_vec = np.array(user_cat + [0.0], dtype=float).reshape(1, -1)
    logger.info("사용자 벡터: %s", user_vec)

    # 2) property_score와 property 테이블을 조인해서 매물 정보 로드
    session = SessionLocal()
    try:
        # 동 필터링 추가: 해당 사용자 dong_id에 해당하는 매물만 조회
        prop_sql = text("""
            SELECT ps.property_id,
                   ps.chicken_score AS safe,
                   ps.leisure_score AS leisure,
                   ps.restaurant_score AS restaurant,
                   ps.health_score AS health,
                   ps.convenience_score AS convenience,
                   ps.transport_score AS transport,
                   ps.cafe_score AS cafe,
                   p.latitude, p.longitude
            FROM property_score ps
            JOIN property p ON ps.property_id = p.property_id
            WHERE p.dong_id = :dong_id
        """)
        props = session.execute(prop_sql, {"dong_id": dong_id}).fetchall()
        logger.info("매물 정보 %d건 조회됨 (dong_id=%s)", len(props), dong_id)
    finally:
        session.close()

    if not props:
        logger.warning("매물 정보가 없습니다.")
        return {"dongId": dong_id, "propertyIds": []}

    # 3) 각 매물 벡터 생성: 카테고리 점수 + 거리 점수
    property_ids = []
    prop_vectors = []
    for r in props:
        pid = r._mapping["property_id"]
        cat_vals = [
            float(r._mapping["safe"] or 0),
            float(r._mapping["leisure"] or 0),
            float(r._mapping["restaurant"] or 0),
            float(r._mapping["health"] or 0),
            float(r._mapping["convenience"] or 0),
            float(r._mapping["transport"] or 0),
            float(r._mapping["cafe"] or 0)
        ]
        plat = float(r._mapping["latitude"] or 0)
        plon = float(r._mapping["longitude"] or 0)
        # 거리 계산
        dist_km = haversine(office_lat, office_lon, plat, plon)
        raw_score = max(0.0, 1.0 - dist_km / 10.0)
        # 가중치 1.5배 적용 후 클리핑
        dist_score = min(1.0, raw_score * 1.5)
        logger.debug("매물ID=%s: 거리=%.2fkm, 원래 점수=%.4f, 가중치 적용 점수=%.4f", pid, dist_km, raw_score, dist_score)
        prop_vec = cat_vals + [dist_score]
        property_ids.append(pid)
        prop_vectors.append(prop_vec)

    prop_matrix = np.array(prop_vectors, dtype=float)
    logger.info("매물 벡터 행렬 shape: %s", prop_matrix.shape)

    # 4) 코사인 유사도 계산
    sims = cosine_similarity(prop_matrix, user_vec).flatten()
    logger.info("유사도 계산 완료. 유사도 예시(상위 10): %s", sims[:10])
    idx_sorted = np.argsort(sims)[::-1][:top_k]
    top_list = [int(property_ids[i]) for i in idx_sorted]
    logger.info("추천 매물 (상위 %d): %s", top_k, top_list)

    return {"dongId": dong_id, "propertyIds": top_list}

