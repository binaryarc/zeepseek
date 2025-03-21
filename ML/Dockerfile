FROM python:3.9-slim

# 터미널 로그가 바로 출력되도록 설정
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# 의존성 파일 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 전체 애플리케이션 코드 복사
COPY ./app /app/app

# uvicorn으로 FastAPI 앱 실행
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
