def scale_score(value: float, min_val: float, max_val: float) -> float:
    if max_val - min_val == 0:
        return 0.0
    scaled = (value - min_val) / (max_val - min_val)
    return round(scaled * 100.0, 4)
