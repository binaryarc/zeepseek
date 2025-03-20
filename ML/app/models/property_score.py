from sqlalchemy import Column, Integer, Float, DateTime, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime

Base = declarative_base()

class PropertyScore(Base):
    __tablename__ = "property_score"
    id = Column(Integer, primary_key=True, autoincrement=True)
    property_id = Column(Integer, ForeignKey("property.property_id"), nullable=False)
    transport_count = Column(Integer, default=0)
    transport_score = Column(Float, default=0)
    restaurant_count = Column(Integer, default=0)
    restaurant_score = Column(Float, default=0)
    health_count = Column(Integer, default=0)
    health_score = Column(Float, default=0)
    convenience_count = Column(Integer, default=0)
    convenience_score = Column(Float, default=0)
    cafe_count = Column(Integer, default=0)
    cafe_score = Column(Float, default=0)
    chicken_count = Column(Integer, default=0)
    chicken_score = Column(Float, default=0)
    leisure_count = Column(Integer, default=0)
    leisure_score = Column(Float, default=0)
    overall_score = Column(Float, default=0)
    updated_at = Column(DateTime, default=datetime.now, onupdate=datetime.now)
