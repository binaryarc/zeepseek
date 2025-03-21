import pandas as pd
import requests
import os
import time
import chardet
import csv
from dotenv import load_dotenv
from concurrent.futures import ThreadPoolExecutor, as_completed

# .env 파일 로드 (카카오 API 키 관리)
load_dotenv()
KAKAO_API_KEY = "e9b65211174553e46ad4148604389c74"

# 파일 인코딩 자동 감지 함수 (일부만 읽어서 감지)
def detect_encoding(file_path, n_bytes=10000):
    with open(file_path, 'rb') as f:
        raw_data = f.read(n_bytes)
    result = chardet.detect(raw_data)
    encoding = result.get('encoding')
    if encoding is None:
        encoding = "cp949"
    return encoding

# CSV 파일의 헤더를 읽어 실제 컬럼명을 추출하는 함수
# 헤더가 한 셀에 모두 들어있는 경우 쉼표(,)로 분리합니다.
def get_actual_columns(file_path, encoding):
    with open(file_path, 'r', encoding=encoding, errors='replace') as f:
        header_line = f.readline().strip()
    # 만약 헤더가 따옴표로 감싸져 있다면 제거
    header_line = header_line.strip('"')
    # 쉼표로 분리
    cols = header_line.split(',')
    # 빈 문자열 제거
    return [col.strip() for col in cols if col.strip() != '']

# CSV 파일 경로
file_path = "오피스텔_with_coords.csv"

# 파일 인코딩 감지 및 출력
detected_encoding = detect_encoding(file_path)
print("Detected encoding:", detected_encoding)

# 실제 컬럼명 추출
actual_cols = get_actual_columns(file_path, detected_encoding)
print("Actual columns:", actual_cols)
num_expected = len(actual_cols)

# CSV 파일 읽기 (구분자는 쉼표로 지정)
try:
    df = pd.read_csv(
        file_path,
        encoding=detected_encoding,
        encoding_errors='replace',
        sep=',',
        engine='python'
    )
except UnicodeDecodeError:
    print("EUC-KR 인코딩에서 문제가 발생했습니다. CP949로 재시도합니다.")
    df = pd.read_csv(
        file_path,
        encoding='cp949',
        encoding_errors='replace',
        sep=',',
        engine='python'
    )

# DataFrame의 컬럼명을 우리가 추출한 실제 컬럼명으로 지정
df.columns = actual_cols
print("DataFrame columns:", df.columns)

# 전체 행 개수
total_rows = len(df)

# 행정동(동) 및 행정구(구) 정보를 가져오는 함수
def get_admin_info_from_coordinates(lat, lng, index, total_rows):
    url = f"https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x={lng}&y={lat}"
    headers = {"Authorization": f"KakaoAK {KAKAO_API_KEY}"}
    try:
        response = requests.get(url, headers=headers, timeout=5)
        if response.status_code == 200:
            result = response.json()
            if result['documents']:
                for doc in result['documents']:
                    # region_type 'H'는 행정동 정보를 의미합니다.
                    if doc['region_type'] == 'H':
                        dong = doc.get('region_3depth_name')
                        gu = doc.get('region_2depth_name')
                        print(f"✅ [{index}/{total_rows}] 변환 성공: 위도 {lat}, 경도 {lng} → 행정동: {dong}, 행정구: {gu}")
                        return dong, gu
        print(f"⚠️ [{index}/{total_rows}] 변환 실패: 위도 {lat}, 경도 {lng}")
    except Exception as e:
        print(f"Error for coordinates ({lat}, {lng}): {e}")
    return None, None

# 멀티스레드를 이용해 각 행의 좌표에 대해 행정동 및 행정구 정보를 조회하는 함수
def fetch_admin_info(idx, row):
    # 'latitude'와 'longitude' 칼럼이 존재해야 합니다.
    dong, gu = get_admin_info_from_coordinates(row['latitude'], row['longitude'], idx+1, total_rows)
    return idx, dong, gu

# ThreadPoolExecutor를 이용한 병렬 처리
dong_names = [None] * total_rows
gu_names = [None] * total_rows
max_workers = 10  # 필요에 따라 조정
with ThreadPoolExecutor(max_workers=max_workers) as executor:
    futures = [executor.submit(fetch_admin_info, idx, row) for idx, row in df.iterrows()]
    for future in as_completed(futures):
        idx, dong, gu = future.result()
        dong_names[idx] = dong
        gu_names[idx] = gu

# 새로운 칼럼 추가
df['dong_name'] = dong_names
df['gu_name'] = gu_names

# 결과 CSV 파일로 저장 (UTF-8-sig 인코딩)
output_path = "off_coords_dongname.csv"
df.to_csv(output_path, index=False, encoding="utf-8-sig")
print(f"\n🎉 행정동 및 행정구 정보 추가 완료! 저장된 파일: {output_path}")
