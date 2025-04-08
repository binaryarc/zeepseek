import random
import json
import requests
import logging
from datetime import datetime, timedelta
from fastapi import APIRouter
from sqlalchemy import text
from app.config.database import SessionLocal

router = APIRouter(prefix="/activity-logs")
# ES 접속 정보
ES_URL = "http://elasticsearch:9200"
ES_USER = "fastapi_user"
ES_PASS = "e203@Password!"

# DB에서 실제 매물 데이터를 조회하여 사용할 리스트를 생성하는 함수 (# 수정됨)
def get_property_list():
    session = SessionLocal()
    try:
        # property 테이블에서 필요한 컬럼들을 조회합니다.
        query = text("""
            SELECT property_id, room_bath_count, room_type, dong_id, price 
            FROM property
        """)
        results = session.execute(query).fetchall()
        properties = []
        for row in results:
            prop = {
                "property_id": row._mapping["property_id"],
                "room_bath_count": row._mapping["room_bath_count"],
                "room_type": row._mapping["room_type"],
                "dong_id": row._mapping["dong_id"],
                "price": row._mapping["price"]
            }
            properties.append(prop)
        return properties
    except Exception as ex:
        logging.error("Error fetching property list: %s", ex)
        return []
    finally:
        session.close()

# computedRoomType 계산 함수 (# 수정됨)
def compute_computed_room_type(room_bath_count: str, room_type: str):
    """
    room_bath_count가 "x/y" 형식일 때,
      - x가 1이면 "원룸"
      - x가 2이면 "투룸"
      - x가 3이면 "쓰리룸"
      - x가 4 이상이면 room_type 컬럼의 값을 그대로 사용
    만약 파싱에 실패하면 room_type을 반환.
    """
    try:
        # "/" 앞의 숫자를 파싱
        room_count = int(room_bath_count.split('/')[0].strip())
        if room_count == 1:
            return "원룸"
        elif room_count == 2:
            return "투룸"
        elif room_count == 3:
            return "쓰리룸"
        else:
            return room_type  # 4 이상인 경우 등
    except Exception as e:
        logging.warning("computed_room_type 계산 실패 (%s): %s", room_bath_count, e)
        return room_type

# 더미 로그 생성 함수 수정 (# 수정됨)
def generate_activity_logs(n=1000):
    """
    n개의 액티비티 로그를 실제 property 테이블의 데이터를 활용하여 생성.
      - propertyId와 dongId는 DB에서 조회한 실제 매물 데이터를 사용
      - computedRoomType은 room_bath_count와 room_type을 바탕으로 결정
      - 성별, 연령, action은 랜덤하게 생성 (성별에 따라 view/zzim 확률 차등 적용)
    """
    logs = []
    property_list = get_property_list()  # 실제 DB에서 매물 데이터를 가져옴
    if not property_list:
        logging.error("매물 데이터를 불러오지 못했습니다.")
        return logs

    for _ in range(n):
        # 0=male, 1=female (여기서 gender 선택)
        gender = random.choice([0, 1])
        # 성별에 따른 action 확률
        if gender == 0:
            # 남성: 70% view, 30% zzim
            action = random.choices(["view", "zzim"], weights=[0.7, 0.3], k=1)[0]
        else:
            # 여성: 40% view, 60% zzim
            action = random.choices(["view", "zzim"], weights=[0.4, 0.6], k=1)[0]
        # 유저ID는 1~5000 사이의 랜덤값
        user_id = random.randint(1, 5000)
        # 나이: 20 ~ 60
        age = random.randint(20, 60)
        # 실제 property_list에서 무작위 매물을 선택
        prop = random.choice(property_list)
        property_id = prop["property_id"]
        dong_id = prop["dong_id"]
        room_bath_count = prop["room_bath_count"]
        room_type = prop["room_type"]
        # computedRoomType 산출 (# 수정됨)
        computed_room_type = compute_computed_room_type(room_bath_count, room_type)
        # 로그 생성: 시간은 최근 30일 범위에서 랜덤하게 선택
        log_entry = {
            "userId": user_id,
            "propertyId": property_id,
            "action": action,
            "age": age,
            "gender": gender,
            "dongId": dong_id,
            "computedRoomType": computed_room_type,  # 추가된 필드
            "time": (datetime.utcnow() - timedelta(days=random.randint(0, 30))).isoformat() + "Z"
        }
        logs.append(log_entry)
    return logs

# Bulk Insert 함수 (변경없음)
def bulk_insert_es(logs, index_name="logs"):
    """ Bulk API로 logs를 ES에 넣는 함수 """
    bulk_data = []
    for doc in logs:
        meta = {"index": {"_index": index_name}}
        bulk_data.append(json.dumps(meta))
        bulk_data.append(json.dumps(doc))
    body = "\n".join(bulk_data) + "\n"
    headers = {"Content-Type": "application/x-ndjson"}
    # ES Basic Auth
    res = requests.post(
        f"{ES_URL}/_bulk",
        data=body,
        headers=headers,
        auth=(ES_USER, ES_PASS),
        verify=False
    )
    print("Status:", res.status_code)
    print("Response:", res.text)

@router.post("/create-logs")
def create_logs_api(count: int = 1000, index_name: str = "logs"):
    """ 
    [POST] /activity-logs/create-logs?count=...&index_name=...
    => count 만큼 실제 DB 데이터를 기반으로 한 로그를 생성하고 ES에 저장합니다.
    """
    logs = generate_activity_logs(count)
    bulk_insert_es(logs, index_name)
    return {"message": f"{count} logs inserted into ES index '{index_name}' successfully!"}
