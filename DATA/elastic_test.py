import requests
import json

# 1. Elasticsearch 클러스터 상태 확인
url = "http://localhost:9200"
response = requests.get(url)
print("클러스터 상태:", json.dumps(response.json(), indent=2))

# 2. 'test-index' 인덱스 생성
index_url = "http://localhost:9200/test-index"
response = requests.put(index_url)
print("인덱스 생성 응답:", json.dumps(response.json(), indent=2))

# 3. 문서 삽입
doc = {
    "title": "Aaaa",
    "content": "a 엘라스틱서치 테스트 키바나 AAA"
}
doc_url = "http://localhost:9200/a-index/_doc"
response = requests.post(doc_url, json=doc)
print("문서 삽입 응답:", json.dumps(response.json(), indent=2))

# 4. 문서 검색
search_url = "http://localhost:9200/test-index/_search"
query = {
    "query": {
        "match": {
            "title": "Test"
        }
    }
}
response = requests.get(search_url, json=query)
print("검색 결과:", json.dumps(response.json(), indent=2))