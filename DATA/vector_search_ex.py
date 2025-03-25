import requests
import json

# 1. 인덱스 생성 및 매핑 정의
index_name = "properties"
mapping = {
  "mappings": {
    "properties": {
      "propertyId": { "type": "integer" },
      "sellerId": { "type": "keyword" },
      "title": { "type": "text" },
      "description": { "type": "text" },
      "price": { "type": "integer" },
      "contractType": { "type": "keyword" },
      "address": { "type": "text" },
      "dongId": { "type": "integer" },
      "latitude": { "type": "float" },
      "longitude": { "type": "float" },
      "categoryVector": {
         "type": "dense_vector",
         "dims": 8
      }
    }
  }
}

url = f"http://localhost:9200/{index_name}"
response = requests.put(url, json=mapping)
print("인덱스 생성 응답:", json.dumps(response.json(), indent=2))


# 2. 10개의 예제 문서 인덱싱
documents = [
    {
      "propertyId": 1,
      "sellerId": "seller_1",
      "title": "안전한 아파트",
      "description": "주변 보안 시스템이 우수한 아파트입니다.",
      "price": 100000,
      "contractType": "매매",
      "address": "서울 강남구 테헤란로 1",
      "dongId": 101,
      "latitude": 37.5,
      "longitude": 127.0,
      "categoryVector": [0.9, 0.3, 0.2, 0.8, 0.5, 0.4, 0.1, 0.0]
    },
    {
      "propertyId": 2,
      "sellerId": "seller_2",
      "title": "여가 시설이 좋은 주택",
      "description": "근처에 공원과 문화시설이 많은 주택입니다.",
      "price": 80000,
      "contractType": "전세",
      "address": "서울 마포구 월드컵북로 10",
      "dongId": 102,
      "latitude": 37.6,
      "longitude": 126.9,
      "categoryVector": [0.3, 0.9, 0.4, 0.2, 0.1, 0.5, 0.7, 0.3]
    },
    {
      "propertyId": 3,
      "sellerId": "seller_3",
      "title": "식당 인근 상가",
      "description": "유동 인구가 많은 식당가 근처 상가입니다.",
      "price": 120000,
      "contractType": "임대",
      "address": "부산 해운대구 센텀중앙로 5",
      "dongId": 201,
      "latitude": 35.2,
      "longitude": 129.1,
      "categoryVector": [0.2, 0.4, 0.9, 0.1, 0.3, 0.6, 0.4, 0.2]
    },
    {
      "propertyId": 4,
      "sellerId": "seller_4",
      "title": "보건 시설 근처 오피스텔",
      "description": "병원 및 보건 시설과 인접한 오피스텔입니다.",
      "price": 90000,
      "contractType": "월세",
      "address": "대구 중구 동성로 15",
      "dongId": 301,
      "latitude": 35.9,
      "longitude": 128.6,
      "categoryVector": [0.7, 0.2, 0.3, 0.9, 0.4, 0.3, 0.2, 0.1]
    },
    {
      "propertyId": 5,
      "sellerId": "seller_5",
      "title": "편의시설 좋은 빌라",
      "description": "편의점, 마트 등 생활 편의시설이 가까운 빌라입니다.",
      "price": 70000,
      "contractType": "매매",
      "address": "인천 남동구 구월로 20",
      "dongId": 401,
      "latitude": 37.4,
      "longitude": 126.7,
      "categoryVector": [0.4, 0.3, 0.2, 0.5, 0.9, 0.4, 0.3, 0.2]
    },
    {
      "propertyId": 6,
      "sellerId": "seller_6",
      "title": "대중교통 접근성 우수한 아파트",
      "description": "지하철과 버스 정류장이 가까운 아파트입니다.",
      "price": 110000,
      "contractType": "전세",
      "address": "서울 동작구 상도로 8",
      "dongId": 501,
      "latitude": 37.5,
      "longitude": 126.9,
      "categoryVector": [0.6, 0.4, 0.3, 0.2, 0.4, 0.9, 0.3, 0.2]
    },
    {
      "propertyId": 7,
      "sellerId": "seller_7",
      "title": "카페 많은 동네의 주택",
      "description": "동네에 유명한 카페들이 즐비한 주택입니다.",
      "price": 85000,
      "contractType": "월세",
      "address": "서울 서대문구 연세로 15",
      "dongId": 601,
      "latitude": 37.6,
      "longitude": 126.94,
      "categoryVector": [0.2, 0.5, 0.3, 0.4, 0.3, 0.4, 0.9, 0.1]
    },
    {
      "propertyId": 8,
      "sellerId": "seller_8",
      "title": "술집 주변 상업공간",
      "description": "술집과 유흥시설이 많은 지역의 상업공간입니다.",
      "price": 95000,
      "contractType": "임대",
      "address": "서울 종로구 종로 25",
      "dongId": 701,
      "latitude": 37.57,
      "longitude": 126.98,
      "categoryVector": [0.3, 0.4, 0.2, 0.3, 0.2, 0.5, 0.1, 0.9]
    },
    {
      "propertyId": 9,
      "sellerId": "seller_9",
      "title": "안전과 여가가 조화로운 아파트",
      "description": "보안과 여가시설이 조화를 이루는 아파트입니다.",
      "price": 105000,
      "contractType": "매매",
      "address": "부산 해운대구 우동 10",
      "dongId": 801,
      "latitude": 35.15,
      "longitude": 129.05,
      "categoryVector": [0.8, 0.8, 0.3, 0.5, 0.3, 0.4, 0.2, 0.1]
    },
    {
      "propertyId": 10,
      "sellerId": "seller_10",
      "title": "종합 생활시설 인근 주택",
      "description": "안전, 여가, 식당, 보건, 편의, 대중교통, 카페, 술집 등 다양한 요소를 겸비한 주택입니다.",
      "price": 115000,
      "contractType": "매매",
      "address": "대전 서구 둔산로 30",
      "dongId": 901,
      "latitude": 36.35,
      "longitude": 127.4,
      "categoryVector": [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5]
    }
]

for doc in documents:
    doc_url = f"http://localhost:9200/{index_name}/_doc"
    response = requests.post(doc_url, json=doc)
    # print("문서 삽입 응답:", json.dumps(response.json(), indent=2))


# 3. 벡터 검색 실행 (유저의 입력 벡터 기준)
# 코사인 유사도를 사용하여 각 문서의 categoryVector와 비교한 후, 유사도가 높은 순으로 결과를 반환합니다.
query = {
  "size": 5,
  "query": {
    "script_score": {
      "query": {
        "match_all": {}
      },
      "script": {
        "source": "cosineSimilarity(params.query_vector, 'categoryVector') + 1.0",
        "params": {
          "query_vector": [0.6, 0.4, 0.3, 0.7, 0.5, 0.3, 0.2, 0.1]
        }
      }
    }
  }
}

search_url = f"http://localhost:9200/{index_name}/_search"
response = requests.get(search_url, json=query)
print("벡터 검색 결과:", json.dumps(response.json(), indent=2))
