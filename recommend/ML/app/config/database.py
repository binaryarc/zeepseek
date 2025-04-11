from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
import os
from dotenv import load_dotenv

load_dotenv()  # .env 파일에 있는 환경 변수 로드

# 환경 변수에서 MYSQL_HOST를 읽고, 없으면 기본값(j12e203.p.ssafy.io)을 사용합니다.
mysql_host = os.getenv("MYSQL_HOST", "j12e203.p.ssafy.io")

# 연결 문자열 생성 (MYSQL_HOST를 사용)
MYSQL_DATABASE_URL = os.getenv(
    "MYSQL_DATABASE_URL",
    f"mysql+pymysql://ssafy:ssafyssafy@{mysql_host}:3306/zeepseek"
)

# 엔진 생성 및 세션 설정
engine = create_engine(
    MYSQL_DATABASE_URL,
    echo=True,
    pool_size=15,        # 기본 커넥션 풀 크기 증가
    max_overflow=15,     # 추가 허용 가능한 커넥션 수
    pool_timeout=30,     # 커넥션을 기다리는 최대 시간(초)
    pool_recycle=1800    # 커넥션 재활용 시간(초, 30분)
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
