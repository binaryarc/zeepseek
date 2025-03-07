## 프로젝트 기획

프로젝트 개요

### 집SEEK

빅데이터를 활용하여 사용자 맞춤형 부동산 추천을 제공하는 서비스

사회초년생과 신혼부부를 주요 타겟으로 하여, 사용자의 선호도 및 조건에 맞는 최적의 거주지를 추천하고 매물 정보를 제공하는 것을 목표로 함.

주요 기능

1. 동네 추천

사용자의 인풋(성별, 나이, 예산, 계약 형태, 대중교통 접근성, 편의시설, 보건, 치안, 가족 구성원 수, 거주 목적 등)에 맞추어 가중치를 계산하여 최적의 동네를 추천

사용자의 선호사항(예: 특정 브랜드 매장, 공원 등)을 반영한 맞춤형 추천 제공

TOP 3 동네 추천 및 해당 동네의 매물 리스트 제공

(후순위) 사용자의 액티비티 로그(클릭 수, 체류 시간 등)를 활용한 추천 개선

목적지와 매물 간의 거리 및 예상 소요 시간 제공

(후순위) 전입신고 가능 여부 체크 기능

(후순위) 등기부등본 분석을 통한 전세 사기 위험도 AI 분석 기능

2. 동네 비교

사용자가 미리 고려한 동네와 추천받은 동네 간의 가중치 차이를 비교하여 더 나은 선택을 할 수 있도록 지원

3. 마이페이지

관심 있는 매물 찜하기 기능

추천받은 동네 찜하기 기능

4. 회원가입

OAuth 기반 간편 로그인 지원

5. 댓글 기능

자소설닷컴 스타일의 자유로운 동네별 댓글 작성 및 열람 기능


#### 매물 데이터 크롤링

- 부동산 플랫폼(네이버 부동산 등)에서 매물 정보 크롤링하여 DB에 저장. 데이터 전처리 후 검색 및 추천 시스템을 위한 데이터셋 구축. 

- 매물 데이터 크롤링 예시 
```
import requests
import json
import pandas as pd

# ✅ 네이버 부동산 매물 API URL (송파구 잠실동 지역 예시)
API_URL = "https://new.land.naver.com/api/complexes/single-markers/2.0"
PARAMS = {
    "cortarNo": "1171010200",  # 송파구 잠실동 지역 코드
    "zoom": "16",
    "priceType": "RETAIL",
    "markerId": "3754",
    "markerType": "COMPLEX",
    "realEstateType": "ABYG:JGC:JGB:OPST:PRE",  # 매물 유형 (아파트, 오피스텔 등)
    "tradeType": "",
    "priceMin": "0",
    "priceMax": "900000000",
    "areaMin": "0",
    "areaMax": "900000000",
    "leftLon": "127.0917269",
    "rightLon": "127.1277757",
    "topLat": "37.5261731",
    "bottomLat": "37.5151109",
    "isPresale": "true"
}

# ✅ 요청 헤더 설정 (네이버 부동산 차단 우회)
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
    "Referer": "https://new.land.naver.com/",
    "Accept": "application/json"
}

# ✅ 네이버 부동산 API 요청
response = requests.get(API_URL, params=PARAMS, headers=HEADERS)

# ✅ 응답 데이터 확인
if response.status_code == 200:
    data = response.json()
    
    # JSON 데이터를 보기 좋게 저장
    with open("naver_real_estate.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)

    # ✅ 매물 정보 리스트 추출
    all_data = []
    for complex_data in data.get("complexMarkers", []):
        try:
            title = complex_data.get("complexName", "이름 없음")
            price = complex_data.get("priceStr", "가격 정보 없음")
            area = complex_data.get("supplyAreaStr", "면적 정보 없음")
            latitude = complex_data.get("latitude", "위도 없음")
            longitude = complex_data.get("longitude", "경도 없음")
            link = f"https://new.land.naver.com/complexes/{complex_data.get('complexNo', '')}"

            all_data.append({
                "매물명": title,
                "가격": price,
                "면적": area,
                "위도": latitude,
                "경도": longitude,
                "링크": link
            })
        except:
            continue

    # ✅ JSON으로 저장
    with open("seoul_real_estate.json", "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=4)

    # ✅ CSV 파일로 저장
    df = pd.DataFrame(all_data)
    df.to_csv("seoul_real_estate.csv", index=False, encoding="utf-8-sig")

    print("✅ 서울 송파구 잠실동 매물 크롤링 완료, JSON 및 CSV 저장됨.")

else:
    print(f"🚨 API 요청 실패! HTTP 상태 코드: {response.status_code}")
```

- JSON 파일 예시

```
[
    {
        "매물명": "송파 잠실 아파트",
        "가격": "15억",
        "면적": "84㎡",
        "위도": 37.5206422,
        "경도": 127.1097513,
        "링크": "https://new.land.naver.com/complexes/3754"
    },
    {
        "매물명": "잠실 트리플 빌라",
        "가격": "7억",
        "면적": "60㎡",
        "위도": 37.5198731,
        "경도": 127.1137463,
        "링크": "https://new.land.naver.com/complexes/3756"
    }
]
```

