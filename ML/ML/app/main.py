import time
import threading
import logging
from fastapi import FastAPI, Request
# 라우터 모듈에서 APIRouter 인스턴스(router)를 가져옵니다.
from app.routers.property_score_router import router as property_score_router
from app.routers.recommend_router import router as ai_recommend_router
from app.modules.generate_logs.generate_logs import router as activity_log_router

# [추가] 모델 학습 함수를 임포트
from app.modules.ai_recommender.recommend_service import train_model, model

app = FastAPI()

# uvicorn 기본 로거 대신 별도의 로거 설정
logger = logging.getLogger("my_app_logger")
logger.setLevel(logging.INFO)
handler = logging.StreamHandler()
formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
handler.setFormatter(formatter)
logger.addHandler(handler)

def periodic_training():
    """
    주기적으로 SVD 모델을 재학습하는 배치 작업 (2시간마다 실행)
    """
    while True:
        try:
            logger.info("==== [Periodic Training] Starting SVD model training batch ====")
            train_model()
            if model.is_trained():
                logger.info("==== [Periodic Training] SVD model training completed successfully ====")
            else:
                logger.warning("==== [Periodic Training] SVD model training did not succeed (or data was empty) ====")
        except Exception as e:
            logger.error("==== [Periodic Training] Error during model training: %s", e)
        # 2시간(7200초) 대기
        time.sleep(7200)

@app.on_event("startup")
def on_startup():
    """
    서버 시작 시 SVD 모델을 학습하고, 주기적인 배치 작업을 백그라운드 스레드로 실행합니다.
    """
    logger.info("==== [Startup] Server is starting up... ====")
    try:
        logger.info("Starting to train the SVD model at startup...")
        train_model()  # 서버 기동 시 한 번 학습
        if model.is_trained():
            logger.info("SVD model is successfully trained at startup.")
        else:
            logger.warning("SVD model training did not succeed or data was empty at startup.")
    except Exception as e:
        logger.error("Error during startup model training: %s", e)
    
    # 주기적 모델 재학습을 위한 백그라운드 스레드 시작 (데몬 스레드로 실행)
    threading.Thread(target=periodic_training, daemon=True).start()

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
app.include_router(activity_log_router)
