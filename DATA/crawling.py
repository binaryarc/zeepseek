from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
import chromedriver_autoinstaller
import pandas as pd
import time
import re
import json
import os

# ✅ ChromeDriver 자동 설치
chromedriver_autoinstaller.install()

# ✅ Selenium 옵션 설정
options = Options()
options.add_argument("--headless")  # 브라우저 창 없이 실행
options.add_argument("--no-sandbox")
options.add_argument("--disable-dev-shm-usage")
options.add_argument("--disable-blink-features=AutomationControlled")
options.add_argument("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")

# ✅ WebDriver 실행
service = Service()
driver = webdriver.Chrome(service=service, options=options)
driver.implicitly_wait(5)  # 🔥 요소 로딩을 기다리는 시간 설정

def load_json(filename):
    if os.path.exists(filename):
        with open(filename, "r", encoding="utf-8") as f:
            return json.load(f)
    else:
        print(f"🚨 파일을 찾을 수 없습니다: {filename}")
        return []

# ✅ 크롤링할 지역 리스트 (JSON 파일 로드)
locations = load_json("seoul_locations.json")

# ✅ 데이터 저장 리스트
all_data = []
SCROLL_LIMIT = 100  # ✅ 최대 스크롤 다운 횟수 설정

for location in locations:
    if location["구"] != "종로구":
        break
    print(f"📌 크롤링 중: {location['구']} {location['동']}")

    # ✅ 해당 법정동 코드로 검색된 네이버 부동산 페이지 접속
    url = f"https://new.land.naver.com/houses?cortarNo={location['법정동코드']}"
    driver.get(url)
    time.sleep(3)

    # ✅ 스크롤 대상 요소 찾기 (`.item_list.item_list--article`)
    try:
        scroll_container = driver.find_element(By.CLASS_NAME, "item_list--article")
    except Exception as e:
        print(f"🚨 스크롤 컨테이너를 찾을 수 없음: {e}")
        continue

    # ✅ 매물 리스트 스크롤하여 추가 매물 로드
    last_count = 0
    scroll_count = 0

    while scroll_count < SCROLL_LIMIT:
        # 현재 로드된 매물 개수 확인
        listings = driver.find_elements(By.CLASS_NAME, "item_link")
        current_count = len(listings)

        # 스크롤 실행
        driver.execute_script("arguments[0].scrollTop = arguments[0].scrollHeight;", scroll_container)
        time.sleep(3)  # 🔥 충분한 로딩 시간 확보

        # 새로운 매물이 로드되었는지 확인
        if current_count == last_count:
            print("🚨 더 이상 로드할 매물이 없습니다.")
            break

        last_count = current_count
        scroll_count += 1
        print(f"🔽 스크롤 다운 {scroll_count}/{SCROLL_LIMIT} - 현재 매물 개수: {current_count}")

    # ✅ 매물 리스트 가져오기
    listings = driver.find_elements(By.CLASS_NAME, "item_link")
    print(f"✅ 총 {len(listings)}개의 매물 발견")

    if not listings:
        print(f"🚨 {location['구']} {location['동']} - 매물 없음")
        continue

    for listing in listings:
        try:
            listing.click()  # ✅ 매물 클릭
            time.sleep(2)  # ✅ 상세 페이지 로딩 대기

            # ✅ 현재 URL에서 articleNo 추출
            current_url = driver.current_url
            match = re.search(r"articleNo=(\d+)", current_url)
            if not match:
                print("🚨 매물 번호(articleNo) 추출 실패")
                continue

            article_no = match.group(1)
            detail_url = f"https://new.land.naver.com/houses?articleNo={article_no}"

            # ✅ 상세 정보 추출
            try:
                유형 = driver.find_element(By.CLASS_NAME, "type").text.strip()
            except:
                유형 = ""

            try:
                거래유형 = driver.find_element(By.XPATH, "//div[contains(@class, 'info_article_price')]/span[1]").text.strip()
            except:
                거래유형 = ""

            try:
                가격 = driver.find_element(By.XPATH, "//div[contains(@class, 'info_article_price')]//span[contains(@class, 'price')]").text.strip()
            except:
                가격 = ""

            try:
                소재지 = driver.find_element(By.XPATH, "//th[contains(text(), '소재지')]/following-sibling::td").text.strip()
            except:
                소재지 = ""

            try:
                매물특징 = driver.find_element(By.XPATH, "//th[contains(text(), '매물특징')]/following-sibling::td").text.strip()
            except:
                매물특징 = ""

            try:
                면적 = driver.find_element(By.XPATH, "//th[contains(text(), '공급/전용면적')]/following-sibling::td").text.strip()
            except:
                면적 = ""

            try:
                층수 = driver.find_element(By.XPATH, "//th[contains(text(), '해당층/총층')]/following-sibling::td").text.strip()
            except:
                층수 = ""

            try:
                방욕실수 = driver.find_element(By.XPATH, "//th[contains(text(), '방수/욕실수')]/following-sibling::td").text.strip()
            except:
                방욕실수 = ""

            try:
                관리비 = driver.find_element(By.XPATH, "//th[contains(text(), '관리비')]/following-sibling::td").text.strip()
            except:
                관리비 = ""

            try:
                입주가능 = driver.find_element(By.XPATH, "//th[contains(text(), '입주가능일')]/following-sibling::td").text.strip()
            except:
                입주가능 = ""

            try:
                방향 = driver.find_element(By.XPATH, "//th[contains(text(), '방향')]/following-sibling::td").text.strip()
            except:
                방향 = ""

            # ✅ 데이터 저장
            all_data.append({
                "매물번호": article_no,
                "구": location["구"],
                "동": location["동"],
                "유형": 유형,
                "거래유형": 거래유형,
                "가격": 가격,
                "소재지": 소재지,
                "매물특징": 매물특징,
                "공급/전용면적": 면적,
                "해당층/총층": 층수,
                "방수/욕실수": 방욕실수,
                "관리비": 관리비,
                "입주 가능일": 입주가능,
                "방향": 방향,
                "URL": detail_url
            })

            print(f"✅ 상세 정보 수집 완료: {소재지} - {가격}")

            time.sleep(2)

        except Exception as e:
            print(f"🚨 상세 페이지 이동 오류: {e}")

    print(f"📌 {location['구']} {location['동']} 크롤링 완료! 총 {len(all_data)}개 매물 수집.")

    # ✅ JSON 파일로 저장
if all_data:
    with open("naver_real_estate_details.json", "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=4)
    print("✅ 모든 매물 상세 크롤링 완료! JSON 파일 저장 완료.")
else:
    print("🚨 크롤링된 데이터가 없습니다. JSON 파일을 저장하지 않습니다.")

# ✅ WebDriver 종료
driver.quit()
