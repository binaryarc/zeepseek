from sqlalchemy.orm import Session
from app.models.property_score import PropertyScore

def get_all_property_scores(db: Session):
    return db.query(PropertyScore).all()
