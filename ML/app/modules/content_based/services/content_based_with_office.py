# 파일: app/modules/content_based/services/content_based_with_office.py

import math
import numpy as np
from sqlalchemy import text
from sklearn.metrics.pairwise import cosine_similarity

# 여기서 haversine 함수를 import
# from app.utils.haversine import haversine
from app.utils.haversine import haversine  # lat1, lon1, lat2, lon2 => float(km)

from app.config.database import SessionLocal

def content_based_with_office_location(user_id: int, top_k=5):
    """
    user_preference:
      - dong_id, safe, leisure, restaurant, health, convenience, transport, cafe
      - latitude, longitude (회사/학교 위치)
    property_score:
      - property_id
      - 7개 카테고리(safe, ... cafe), + 매물 lat/lon

    로직:
      1) user_preference에서 사용자 dong_id, 7개 카테고리(0/1), office_lat/office_lon 얻기
      2) property_score에서 각 매물의 7개 카테고리, lat/lon
      3) distance = haversine(office_lat, office_lon, prop_lat, prop_lon)
         => 예시로 거리 점수를 max(0, 1 - distance/10) 계산
         => 최종 매물 벡터 = [7개 카테고리, dist_score] => 8차원
      4) 사용자 벡터: [7개 카테고리, 0.0] => 8차원
      5) 코사인 유사도 => 상위 top_k
      6) 반환 {"dongId": ..., "propertyIds": [...]}
    """
    session = SessionLocal()
    try:
        # 1) user_preference 조회
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
            return {"dongId": None, "propertyIds": []}

        dong_id = user_row._mapping["dong_id"]
        user_cat = [
            float(user_row._mapping["safe"] or 0),
            float(user_row._mapping["leisure"] or 0),
            float(user_row._mapping["restaurant"] or 0),
            float(user_row._mapping["health"] or 0),
            float(user_row._mapping["convenience"] or 0),
            float(user_row._mapping["transport"] or 0),
            float(user_row._mapping["cafe"] or 0)
        ]
        office_lat = float(user_row._mapping["office_lat"] or 0)
        office_lon = float(user_row._mapping["office_lon"] or 0)

    finally:
        session.close()

    # 사용자 벡터 (8차원)
    # 7개 카테고리 + 거리점수(초기=0.0)
    user_vec = np.array(user_cat + [0.0], dtype=float).reshape(1, -1)

    # 2) property_score 로드
    session = SessionLocal()
    try:
        prop_sql = text("""
            SELECT property_id,
                   safe, leisure, restaurant, health,
                   convenience, transport, cafe,
                   latitude, longitude
            FROM property_score
        """)
        props = session.execute(prop_sql).fetchall()
    finally:
        session.close()

    if not props:
        return {"dongId": dong_id, "propertyIds": []}

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

        # 회사/학교 거리
        dist_km = haversine(office_lat, office_lon, plat, plon)
        # 예시: 10km 이하 => 1..0, 그 이상 => 0
        dist_score = max(0.0, 1.0 - dist_km/10.0)

        prop_vec = cat_vals + [dist_score]
        property_ids.append(pid)
        prop_vectors.append(prop_vec)

    prop_matrix = np.array(prop_vectors, dtype=float)  # shape=(N,8)

    # 코사인 유사도
    sims = cosine_similarity(prop_matrix, user_vec).flatten()
    idx_sorted = np.argsort(sims)[::-1][:top_k]
    top_list = [int(property_ids[i]) for i in idx_sorted]

    return {"dongId": dong_id, "propertyIds": top_list}
