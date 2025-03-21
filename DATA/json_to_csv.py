import json
import csv

# JSON 파일 경로와 CSV 파일 경로 설정
json_file = '오피스텔.json'
csv_file = '오피스텔텔.csv'

# JSON 데이터 읽기
with open(json_file, 'r', encoding='utf-8') as f:
    data = json.load(f)

# CSV 파일 생성 (UTF-8 BOM 포함)
with open(csv_file, 'w', newline='', encoding='utf-8-sig') as f:
    writer = csv.writer(f)
    
    # 헤더 작성 (영어 컬럼명)
    writer.writerow([
        'room_type',          # 매물유형
        'contract_type',     # 거래유형
        'price',              # 가격
        'address',            # 소재지
        'description',        # 매물특징
        'area',               # 공급/전용면적
        'floor_info',         # 해당층/총층
        'room_bath_count',    # 방수/욕실수
        'maintenance_fee',    # 관리비
        'move_in_date',       # 입주 가능일
        'direction',          # 방향
        'image_url',          # 메인사진
        'sale_price',         # 매매가(만원)
        'deposit',            # 보증금(만원)
        'monthly_rent',       # 월세(만원)
        'latitude',           # lat
        'longitude',          # lon
        'dong_name',          # 행정동명
        'gu_name'             # 행정구명
    ])
    
    # JSON 데이터 각 항목을 CSV의 한 행으로 기록
    for item in data:
        writer.writerow([
            item.get("매물유형", ""),
            item.get("거래유형", ""),
            item.get("가격", ""),
            item.get("소재지", ""),
            item.get("매물특징", ""),
            item.get("공급/전용면적", ""),
            item.get("해당층/총층", ""),
            item.get("방수/욕실수", ""),
            item.get("관리비", ""),
            item.get("입주 가능일", ""),
            item.get("방향", ""),
            item.get("메인사진", ""),
            item.get("매매가(만원)", ""),
            item.get("보증금(만원)", ""),
            item.get("월세(만원)", "")
        ])