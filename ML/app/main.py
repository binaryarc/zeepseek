import time
import logging
from fastapi import FastAPI, Request

app = FastAPI()

# 기본 uvicorn 로거 외에도, 필요한 로거를 설정할 수 있습니다.
logger = logging.getLogger("uvicorn.access")

@app.middleware("http")
async def logging_middleware(request: Request, call_next):
    start_time = time.time()
    client_ip = request.client.host if request.client else "unknown"
    # 요청 로그
    logger.info(f"[Request] {request.method} {request.url} from {client_ip}")
    
    # 다음 미들웨어 또는 엔드포인트 호출
    response = await call_next(request)
    
    duration_ms = int((time.time() - start_time) * 1000)
    # 응답 로그
    logger.info(f"[Response] {request.method} {request.url} -> {response.status_code} [{duration_ms}ms]")
    return response

@app.get("/")
def read_root():
    return {"message": "Real Estate Recommendation System API"}
