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
    """
    user_preference:
      - dong_id, safe, leisure, restaurant, health, convenience, transport, cafe
      - latitude, longitude (회사/학교 위치) <- user_preference 테이블에 저장된 값 사용
    property_score:
      - property_id
      - 7개 카테고리: (safe, leisure, restaurant, health, convenience, transport, cafe)
         (단, property_score 테이블에는 safe 컬럼이 없으므로 chicken_score를 safe로 사용)
    property:
      - latitude, longitude (매물 위치)
    로직:
      1) user_preference에서 사용자 dong_id, 7개 카테고리, office_lat/office_lon 얻기
      2) property_score와 property 테이블을 조인해서 각 매물의 7개 카테고리와 위치(lat, lon) 로드
      3) distance = haversine(office_lat, office_lon, prop_lat, prop_lon)
         → 거리 점수 = max(0, 1 - distance/10)
         → 최종 매물 벡터 = [7개 카테고리, dist_score] (8차원)
      4) 사용자 벡터 = [7개 카테고리, 0.0] (8차원)
      5) 코사인 유사도 계산 후 상위 top_k 매물 선택
      6) 반환: {"dongId": <user의 dong_id>, "propertyIds": [매물ID1, 매물ID2, ...]}
    """
    logger.info("=== content_based_with_office_location 시작: user_id=%d, top_k=%d ===", user_id, top_k)
    
    # 1) 사용자 정보 로드: user_preference 테이블에서 사용자 dong_id, 카테고리, office 위치
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
        # safe 컬럼은 property_score에 없으므로 chicken_score를 safe로 alias 처리하고,
        # property 테이블의 latitude, longitude를 가져옵니다.
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
        """)
        props = session.execute(prop_sql).fetchall()
        logger.info("매물 정보 %d건 조회됨", len(props))
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
        # property 테이블에서 가져온 위치 정보 사용
        plat = float(r._mapping["latitude"] or 0)
        plon = float(r._mapping["longitude"] or 0)
        # 3) 회사/학교와 매물 간 거리 계산 (단위: km)
        dist_km = haversine(office_lat, office_lon, plat, plon)
        dist_score = max(0.0, 1.0 - dist_km / 10.0)
        logger.debug("매물ID=%s: 거리=%.2fkm, 거리점수=%.4f", pid, dist_km, dist_score)
        # 최종 매물 벡터: 7개 카테고리 + 거리 점수 (8차원)
        prop_vec = cat_vals + [dist_score]
        property_ids.append(pid)
        prop_vectors.append(prop_vec)

    prop_matrix = np.array(prop_vectors, dtype=float)
    logger.info("매물 벡터 행렬 shape: %s", prop_matrix.shape)

    # 4) 코사인 유사도 계산: 매물 벡터와 사용자 벡터 간 유사도
    sims = cosine_similarity(prop_matrix, user_vec).flatten()
    logger.info("유사도 계산 완료. 유사도 예시(상위 10): %s", sims[:10])
    idx_sorted = np.argsort(sims)[::-1][:top_k]
    top_list = [int(property_ids[i]) for i in idx_sorted]
    logger.info("추천 매물 (상위 %d): %s", top_k, top_list)

    return {"dongId": dong_id, "propertyIds": top_list}
