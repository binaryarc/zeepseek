from sqlalchemy import text
from app.config.database import SessionLocal

def get_score_min_max_values():
    session = SessionLocal()
    try:
        query = text("""
            SELECT 
                MIN(transport_score) AS min_transport, MAX(transport_score) AS max_transport,
                MIN(restaurant_score) AS min_restaurant, MAX(restaurant_score) AS max_restaurant,
                MIN(health_score) AS min_health, MAX(health_score) AS max_health,
                MIN(convenience_score) AS min_convenience, MAX(convenience_score) AS max_convenience,
                MIN(cafe_score) AS min_cafe, MAX(cafe_score) AS max_cafe,
                MIN(chicken_score) AS min_chicken, MAX(chicken_score) AS max_chicken,
                MIN(leisure_score) AS min_leisure, MAX(leisure_score) AS max_leisure
            FROM property_score
        """)
        row = session.execute(query).fetchone()
        return {
            "transport_score": (row._mapping["min_transport"], row._mapping["max_transport"]),
            "restaurant_score": (row._mapping["min_restaurant"], row._mapping["max_restaurant"]),
            "health_score": (row._mapping["min_health"], row._mapping["max_health"]),
            "convenience_score": (row._mapping["min_convenience"], row._mapping["max_convenience"]),
            "cafe_score": (row._mapping["min_cafe"], row._mapping["max_cafe"]),
            "chicken_score": (row._mapping["min_chicken"], row._mapping["max_chicken"]),
            "leisure_score": (row._mapping["min_leisure"], row._mapping["max_leisure"]),
        }
    except Exception as e:
        print(f"[Error] Failed to get min-max: {e}")
        return {}
    finally:
        session.close()
