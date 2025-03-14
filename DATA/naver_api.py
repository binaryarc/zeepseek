# 네이버 검색 API 예제 - 블로그 검색
import os
import sys
import requests
from pprint import pprint
import urllib.request
client_id = "XHf7pHqSqya5MPGaw5ZT"
client_secret = "ERVD6DtFi5"

responses = []        # 각 호출의 결과를 저장할 리스트

# 최대 반복 횟수를 20회로 고정
for i in range(1, 21):
    # 첫 호출은 start=1, 그 이후에는 5, 10, 15, ... 로 설정
    start = i * 5
    url = "https://openapi.naver.com/v1/search/local.json"
    headers = {
        "X-Naver-Client-Id": client_id,
        "X-Naver-Client-Secret": client_secret
    }
    params = {
        "query": "도봉구 쌍문동 병원",  # 검색어
        "display": 5,    # 한 페이지에 보여줄 결과 수 (최대 10)
        "start": start        # 검색 시작 위치
    }
    
    try:
        response = requests.get(url, headers=headers, params=params)
        if response.status_code == 200:
            data = response.json()
            responses.append(data)
            print(f"start={start} -> 결과 저장")
        else:
             print("API 요청 실패:", response.status_code, response.text)
    except Exception as e:
        print("Exception 발생:", e)

# responses 리스트에 20회 호출 결과가 저장되어 있음
print("결과:")
pprint(responses)