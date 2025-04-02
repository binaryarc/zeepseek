from sqlalchemy import text
from app.config.database import SessionLocal
from app.modules.property_scoring.dynamic_score_min_max import get_score_min_max_values
from app.utils.normalization_utils import scale_score
from app.utils.logger import logger

def normalize_scores_and_update():
    min_max_dict = get_score_min_max_values()
    session = SessionLocal()

    try:
        rows = session.execute(text("""
            SELECT property_id,
                   transport_score, restaurant_score, health_score, convenience_score,
                   cafe_score, chicken_score, leisure_score
            FROM property_score
        """)).fetchall()

        for row in rows:
            pid = row._mapping["property_id"]
            update_data = {}

            for category in [
                "transport_score", "restaurant_score", "health_score",
                "convenience_score", "cafe_score", "chicken_score", "leisure_score"
            ]:
                original = row._mapping[category]
                min_val, max_val = min_max_dict.get(category, (0, 1))
                scaled = scale_score(original, min_val, max_val)
                update_data[category] = scaled

            stmt = text("""
                UPDATE property_score
                SET transport_score = :transport_score,
                    restaurant_score = :restaurant_score,
                    health_score = :health_score,
                    convenience_score = :convenience_score,
                    cafe_score = :cafe_score,
                    chicken_score = :chicken_score,
                    leisure_score = :leisure_score
                WHERE property_id = :property_id
            """)
            session.execute(stmt, {**update_data, "property_id": pid})

        session.commit()
        logger.info("[Normalization] Successfully updated scores with normalization.")
    except Exception as e:
        session.rollback()
        logger.error("[Normalization Error] %s", e)
    finally:
        session.close()
