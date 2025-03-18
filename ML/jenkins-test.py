from fastapi import FastAPI
import mysql.connector
import asyncio
import os
from pymongo import MongoClient
from elasticsearch import Elasticsearch

# 전역 변수 초기화
mysql_conn = None
mysql_cursor = None
mongo_client = None
mongo_db = None
mongo_collection = None
es = None
ES_INDEX = "test_index"

async def lifespan(app: FastAPI):
    global mysql_conn, mysql_cursor, mongo_client, mongo_db, mongo_collection, es
    max_attempts = 10

    # MySQL 연결 (재시도 로직 포함)
    mysql_attempt = 0
    MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
    MYSQL_USER = "ssafy"
    MYSQL_PASSWORD = "ssafyssafy"
    MYSQL_DATABASE = "ssafy"
    while mysql_attempt < max_attempts:
        try:
            mysql_conn = mysql.connector.connect(
                host=MYSQL_HOST,
                user=MYSQL_USER,
                password=MYSQL_PASSWORD,
                database=MYSQL_DATABASE
            )
            mysql_cursor = mysql_conn.cursor()
            print("MySQL 연결 성공!")
            break
        except mysql.connector.Error as e:
            mysql_attempt += 1
            print(f"MySQL 연결 실패 ({mysql_attempt}/{max_attempts}), 5초 후 재시도합니다...", e)
            await asyncio.sleep(5)
    else:
        raise Exception("MySQL 연결에 실패했습니다. 최대 재시도 횟수를 초과했습니다.")

    # MongoDB 연결 (재시도 로직 포함)
    mongo_attempt = 0
    MONGO_HOST = os.getenv("MONGO_HOST", "localhost")
    MONGO_PORT = 27017
    MONGO_DATABASE = "test_db"
    while mongo_attempt < max_attempts:
        try:
            mongo_client = MongoClient(f"mongodb://{MONGO_HOST}:{MONGO_PORT}/")
            mongo_db = mongo_client[MONGO_DATABASE]
            mongo_collection = mongo_db["test_collection"]
            # 연결 테스트 (ping)
            mongo_client.admin.command("ping")
            print("MongoDB 연결 성공!")
            break
        except Exception as e:
            mongo_attempt += 1
            print(f"MongoDB 연결 실패 ({mongo_attempt}/{max_attempts}), 5초 후 재시도합니다...", e)
            await asyncio.sleep(5)
    else:
        raise Exception("MongoDB 연결에 실패했습니다. 최대 재시도 횟수를 초과했습니다.")

    # Elasticsearch 연결 (재시도 로직 포함)
    es_attempt = 0
    ES_HOST = os.getenv("ES_HOST", "localhost")
    ES_PORT = 9200
    while es_attempt < max_attempts:
        try:
            es = Elasticsearch(f"http://{ES_HOST}:{ES_PORT}")
            if es.ping():
                print("Elasticsearch 연결 성공!")
                break
            else:
                raise Exception("Elasticsearch ping 실패")
        except Exception as e:
            es_attempt += 1
            print(f"Elasticsearch 연결 실패 ({es_attempt}/{max_attempts}), 5초 후 재시도합니다...", e)
            await asyncio.sleep(5)
    else:
        raise Exception("Elasticsearch 연결에 실패했습니다. 최대 재시도 횟수를 초과했습니다.")

    yield

    # 종료(cleanup) 시 자원 정리
    if mysql_cursor:
        mysql_cursor.close()
    if mysql_conn:
        mysql_conn.close()
    if mongo_client:
        mongo_client.close()
    # Elasticsearch는 별도의 종료 메서드가 없습니다.

app = FastAPI(lifespan=lifespan)

### 1. MySQL API ###
@app.post("/mysql/insert/")
def insert_mysql(name: str, age: int):
    query = "INSERT INTO users (name, age) VALUES (%s, %s)"
    mysql_cursor.execute(query, (name, age))
    mysql_conn.commit()
    return {"message": "MySQL Insert Success"}

@app.get("/mysql/select/")
def select_mysql():
    mysql_cursor.execute("SELECT * FROM users")
    result = mysql_cursor.fetchall()
    return {"data": result}

### 2. MongoDB API ###
@app.post("/mongo/insert/")
def insert_mongo(name: str, age: int):
    data = {"name": name, "age": age}
    mongo_collection.insert_one(data)
    return {"message": "MongoDB Insert Success"}

@app.get("/mongo/select/")
def select_mongo():
    result = list(mongo_collection.find({}, {"_id": 0}))
    return {"data": result}

### 3. Elasticsearch API ###
@app.post("/es/insert/")
def insert_es(name: str, age: int):
    data = {"name": name, "age": age}
    res = es.index(index=ES_INDEX, body=data)
    return {"message": "Elasticsearch Insert Success", "result": res}

@app.get("/es/select/")
def select_es():
    res = es.search(index=ES_INDEX, body={"query": {"match_all": {}}})
    return {"data": res["hits"]["hits"]}