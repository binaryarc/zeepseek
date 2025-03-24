from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
import os
from dotenv import load_dotenv

load_dotenv()  # .env 파일에 있는 환경 변수 로드

MYSQL_DATABASE_URL = os.getenv("MYSQL_DATABASE_URL", "mysql+pymysql://ssafy:ssafyssafy@j12e203.p.ssafy.io:3306/zeepseek")
engine = create_engine(MYSQL_DATABASE_URL, echo=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
engine = create_engine(
    MYSQL_DATABASE_URL,
    echo=True,
    pool_size=15,        # 기본 커넥션 풀 크기 증가
    max_overflow=15,     # 추가 허용 가능한 커넥션 수
    pool_timeout=30,     # 커넥션을 기다리는 최대 시간(초)
    pool_recycle=1800    # 커넥션 재활용 시간(초, 30분)
)