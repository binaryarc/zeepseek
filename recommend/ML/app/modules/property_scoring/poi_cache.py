import time
import threading
import numpy as np
from sklearn.neighbors import BallTree
from sqlalchemy import text
from app.config.database import SessionLocal
from app.utils.logger import logger

CACHE_TTL = 3600  # 1시간 (초)
POI_CACHE = {}          # {category: [ {lat, lon}, ... ]}
BALLTREE_CACHE = {}     # {category: BallTree}
CACHE_TIMESTAMP = {}    # {category or category+'_ball': time.time()}
CACHE_LOCK = threading.Lock()

def is_cache_valid(category_key: str) -> bool:
    """ 캐시된 시점(CACHE_TIMESTAMP)으로부터 1시간이 경과했는지 체크 """
    now = time.time()
    last_ts = CACHE_TIMESTAMP.get(category_key, 0)
    return (now - last_ts) < CACHE_TTL

def get_poi_by_category(category: str, session=None):
    """
    POI 데이터를 DB에서 조회 후, TTL(1시간) 이내면 캐시 재사용.
    """
    global POI_CACHE
    cat_key = f"poi_{category}"

    # 캐시에 있고 유효하면 반환
    if category in POI_CACHE and is_cache_valid(cat_key):
        return POI_CACHE[category]

    with CACHE_LOCK:
        # 더블체크
        if category in POI_CACHE and is_cache_valid(cat_key):
            return POI_CACHE[category]

        close_session = False
        if session is None:
            session = SessionLocal()
            close_session = True
        try:
            if category == "transport":
                query = text("SELECT latitude, longitude FROM transport")
            elif category == "restaurant":
                query = text("SELECT latitude, longitude FROM restaurant")
            elif category == "health":
                query = text("SELECT latitude, longitude FROM health")
            elif category == "convenience":
                query = text("SELECT latitude, longitude FROM convenience")
            elif category == "cafe":
                query = text("SELECT latitude, longitude FROM cafe")
            elif category == "chicken":
                query = text("SELECT latitude, longitude FROM chicken")
            elif category == "leisure":
                query = text("SELECT latitude, longitude FROM leisure")
            else:
                POI_CACHE[category] = []
                CACHE_TIMESTAMP[cat_key] = time.time()
                return []

            result = session.execute(query).fetchall()
            poi_list = [
                {"latitude": float(row._mapping["latitude"]), "longitude": float(row._mapping["longitude"])}
                for row in result
            ]
            POI_CACHE[category] = poi_list
            CACHE_TIMESTAMP[cat_key] = time.time()
            return poi_list
        finally:
            if close_session:
                session.close()

def get_balltree_for_category(category: str, session=None):
    """
    카테고리별 BallTree 구성 후, TTL(1시간) 이내라면 캐시 재사용.
    """
    global BALLTREE_CACHE
    ball_key = f"ball_{category}"

    if category in BALLTREE_CACHE and is_cache_valid(ball_key):
        return BALLTREE_CACHE[category]

    poi_list = get_poi_by_category(category, session=session)
    if not poi_list:
        BALLTREE_CACHE[category] = None
        CACHE_TIMESTAMP[ball_key] = time.time()
        return None

    coords = np.radians(np.array([[p["latitude"], p["longitude"]] for p in poi_list]))
    tree = BallTree(coords, metric='haversine')
    BALLTREE_CACHE[category] = tree
    CACHE_TIMESTAMP[ball_key] = time.time()
    return tree

def initialize_poi_cache():
    """
    애플리케이션 시작 시, 주요 카테고리의 POI/BallTree 미리 로드. 
    """
    categories = ["transport", "restaurant", "health", "convenience", "cafe", "chicken", "leisure"]
    session = SessionLocal()
    try:
        for cat in categories:
            get_poi_by_category(cat, session=session)
            get_balltree_for_category(cat, session=session)
        logger.info("[POI_CACHE] All categories cached with TTL=1h.")
    finally:
        session.close()
