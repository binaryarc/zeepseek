from sqlalchemy import Column, Integer, Float, DateTime, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime

Base = declarative_base()

class PropertyScore(Base):
    __tablename__ = "property_score"
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    property_id = Column(Integer, ForeignKey("property.property_id"), nullable=False)
    
    # 각 POI 카테고리에 대해 반경 내 시설 개수와 거리 기반 점수를 저장합니다.
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
    
    updated_at = Column(DateTime, default=datetime.now, onupdate=datetime.now)
