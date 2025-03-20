from sqlalchemy import Column, Integer, String, Text, Float, DateTime, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime

Base = declarative_base()

class Property(Base):
    __tablename__ = "property"
    
    property_id = Column(Integer, primary_key=True, autoincrement=True)
    seller_id = Column(Integer, nullable=False)
    title = Column(String(255), nullable=False)
    description = Column(Text)
    price = Column(Integer, nullable=False)
    contract_type = Column(String(20), nullable=False)  # 예: 전세, 월세, 매매
    room_type = Column(String(20), nullable=False)      # 예: 원룸, 투룸, 빌라, 아파트
    address = Column(String(255), nullable=False)
    dong_id = Column(Integer, ForeignKey("dong.dong_id"), nullable=False)
    latitude = Column(Float)
    longitude = Column(Float)
    created_at = Column(DateTime, default=datetime.now)
