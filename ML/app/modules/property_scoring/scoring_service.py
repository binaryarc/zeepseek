import numpy as np
from app.modules.property_scoring.poi_cache import get_balltree_for_category
from app.utils.logger import logger
from sklearn.neighbors import BallTree

def distance_score(distance: float) -> float:
    """ 거리 -> 1 / (1 + distance)로 점수화 """
    return 1.0 / (1.0 + distance)

def calculate_category_score_with_balltree(
    lat: float,
    lon: float,
    category: str,
    radius: float = 1.0,
    alpha: float = 0.5,
    beta: float = 0.5,
    session=None
):
    """
    BallTree 활용: 1km 반경 내 POI 개수, 가장 가까운 거리 => 점수 계산
    """
    tree = get_balltree_for_category(category, session=session)
    if tree is None:
        base_score = distance_score(radius)
        return 0, base_score, alpha*0 + beta*base_score

    point = np.radians(np.array([[lat, lon]]))
    radius_rad = radius / 6371.0

    indices = tree.query_radius(point, r=radius_rad)
    count = len(indices[0])
    if count == 0:
        min_dist = radius
    else:
        dist_array, _ = tree.query(point, k=count)
        min_dist = float(dist_array.min()) * 6371.0

    dscore = distance_score(min_dist)
    cat_score = alpha*count + beta*dscore
    return count, dscore, cat_score

def compute_property_score(data: dict, session=None) -> dict:
    """
    매물(data={latitude, longitude})에 대한 카테고리 점수 계산 (BallTree)
    """
    lat, lon = data["latitude"], data["longitude"]
    categories = {
        "transport":    {"alpha":0.5, "beta":0.5},
        "restaurant":   {"alpha":0.5, "beta":0.5},
        "health":       {"alpha":0.5, "beta":0.5},
        "convenience":  {"alpha":0.5, "beta":0.5},
        "cafe":         {"alpha":0.5, "beta":0.5},
        "chicken":      {"alpha":0.2, "beta":0.2},
        "leisure":      {"alpha":0.5, "beta":0.3}
    }
    result = {}
    for cat, w in categories.items():
        c, ds, score = calculate_category_score_with_balltree(
            lat, lon, cat, 1.0,
            alpha=w["alpha"], beta=w["beta"],
            session=session
        )
        result[f"{cat}_count"] = c
        result[f"{cat}_score"] = score

    return result
