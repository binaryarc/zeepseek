import pandas as pd
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed

# 본인의 카카오 REST API 키로 변경하세요.
KAKAO_API_KEY = "38c66a61f9b3af37dec7da2f800c1199"

def get_lat_lng(address):
    headers = {"Authorization": f"KakaoAK {KAKAO_API_KEY}"}
    params = {"query": address}
    url = "https://dapi.kakao.com/v2/local/search/address.json"
    try:
        response = requests.get(url, headers=headers, params=params)
        if response.status_code == 200:
            data = response.json()
            if data["documents"]:
                # documents의 첫 번째 결과에서 x는 경도(longitude), y는 위도(latitude)
                doc = data["documents"][0]
                return doc["y"], doc["x"]
        # 결과가 없거나 오류인 경우
        return None, None
    except Exception as e:
        print(f"Error for address '{address}': {e}")
        return None, None

def fetch_coordinates(address, cache):
    # 캐시에 값이 있으면 바로 리턴
    if address in cache:
        return address, cache[address]
    # API 호출 후 캐시에 저장
    coords = get_lat_lng(address)
    cache[address] = coords
    return address, coords

if __name__ == "__main__":
    # CSV 파일 읽기 (파일명과 인코딩을 상황에 맞게 조정)
    df = pd.read_csv("오피스텔.csv", encoding="utf-8-sig")
    
    # 주소 컬럼에서 고유한 주소 목록 추출
    unique_addresses = df["address"].unique()
    cache = {}  # 주소별 좌표 결과 캐싱용 딕셔너리
    
    # 병렬 처리를 위한 ThreadPoolExecutor 사용 (max_workers는 API 호출 제한에 맞게 조정)
    results = {}
    max_workers = 10
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # 각 고유 주소에 대해 fetch_coordinates 함수 호출
        future_to_address = {
            executor.submit(fetch_coordinates, address, cache): address
            for address in unique_addresses
        }
        for future in as_completed(future_to_address):
            address = future_to_address[future]
            try:
                addr, coords = future.result()
                results[addr] = coords
                print(f"✅ 완료: '{addr}' → {coords}")
            except Exception as e:
                print(f"⚠️ 에러 발생: '{address}' → {e}")
    
    # DataFrame의 각 행에 대해 캐싱된 결과로 위도, 경도 리스트 구성
    latitudes = []
    longitudes = []
    for addr in df["address"]:
        lat, lng = results.get(addr, (None, None))
        latitudes.append(lat)
        longitudes.append(lng)
    
    # 새 컬럼 추가
    df["latitude"] = latitudes
    df["longitude"] = longitudes
    
    # 결과 CSV 파일 저장 (utf-8-sig 인코딩)
    df.to_csv("오피스텔_with_coords.csv", index=False, encoding="utf-8-sig")
    
    print("🎉 CSV 파일에 위도, 경도 컬럼 추가 완료!")