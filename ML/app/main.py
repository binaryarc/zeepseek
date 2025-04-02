import time
import logging
from fastapi import FastAPI, Request
# 라우터 모듈에서 APIRouter 인스턴스(router)를 가져옵니다.
from app.routers.property_score_router import router as property_score_router
from app.routers.recommend_router import router as ai_recommend_router

app = FastAPI()

# uvicorn 기본 로거 대신 별도의 로거 설정
logger = logging.getLogger("my_app_logger")
logger.setLevel(logging.INFO)
handler = logging.StreamHandler()
formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
handler.setFormatter(formatter)
logger.addHandler(handler)

@app.middleware("http")
async def logging_middleware(request: Request, call_next):
    start_time = time.time()
    client_ip = request.client.host if request.client else "unknown"
    logger.info(f"[Request] {request.method} {request.url} from {client_ip}")

    # 요청 본문 읽기 시도 및 예외 처리
    try:
        body_bytes = await request.body()
    except Exception as e:
        logger.error(f"Error reading request body: {e}")
        body_bytes = b""
    
    if body_bytes:
        try:
            body_text = body_bytes.decode("utf-8")
        except Exception as e:
            logger.error(f"Error decoding request body: {e}")
            body_text = "<undecodable>"
        logger.info(f"[Request Body] {body_text}")
    else:
        logger.info("[Request Body] None")
    
    # 엔드포인트에서 요청 본문을 다시 읽을 수 있도록 복원
    async def receive():
        return {"type": "http.request", "body": body_bytes}
    request._receive = receive

    response = await call_next(request)
    
    duration_ms = int((time.time() - start_time) * 1000)
    logger.info(f"[Response] {request.method} {request.url} -> {response.status_code} [{duration_ms}ms]")
    return response

@app.get("/")
def read_root():
    return {"message": "Real Estate Recommendation System API"}

# 올바른 APIRouter 인스턴스를 등록합니다.
app.include_router(property_score_router)
app.include_router(ai_recommend_router)