from sqlalchemy import Column, Integer, String, Text, Float, DateTime, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime

Base = declarative_base()

class Property(Base):
    __tablename__ = "property"
    
    property_id = Column(Integer, primary_key=True, autoincrement=True)
    seller_id = Column(Integer, nullable=False)
    room_type = Column(String(20), nullable=False)          # 예: 원룸, 투룸, 빌라, 아파트
    contract_type = Column(String(20), nullable=False)        # 예: 전세, 월세, 매매
    price = Column(String(30), nullable=False)                # 가격 (문자열)
    address = Column(String(255), nullable=False)
    description = Column(Text)
    area = Column(String(50))
    floor_info = Column(String(50))
    room_bath_count = Column(String(50))
    maintenance_fee = Column(Integer, default=0)
    move_in_date = Column(String(50))
    direction = Column(String(50))
    image_url = Column(Text)
    sale_price = Column(Integer)
    deposit = Column(Integer)
    monthly_rent = Column(Integer)
    latitude = Column(Float)
    longitude = Column(Float)
    dong_id = Column(Integer, ForeignKey("dong.dong_id"), nullable=False)
    gu_name = Column(String(10), nullable=False)
    created_at = Column(DateTime, default=datetime.now)
