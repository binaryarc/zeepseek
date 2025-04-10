# zeepseek (집 Seek)

![logo.png](zeepseek_logo.png)  
- 사회초년생 및 서울 이사가 처음인 모든 사람을 위한 부동산 매물 빅데이터 기반 추천 웹 사이트  
- 기간: 2025.02.24 ~ 2025.04.11

---

## 팀원 소개

| **전희성**             | **백승훈**           | **이진호**             | **박수민**           | **하건수**           | **이원재**           |
|-----------------------|---------------------|-----------------------|---------------------|---------------------|---------------------|
| - 팀장<br>- 백엔드 개발<br>- 데이터 처리 | - 백엔드 개발<br>- 인프라 | - 백엔드 개발<br>- 데이터 처리 | - 프론트 개발<br>- 데이터 크롤링 | - 프론트 개발<br>- 데이터 크롤링 | - 프론트 개발<br>- 데이터 처리 |

---

## 기술스택

### Frontend

<img src="https://img.shields.io/badge/JavaScript-ES6-F7DF1E?style=for-the-badge&logo=JavaScript&logoColor=white" alt="JavaScript">  
<img src="https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=CSS3&logoColor=white" alt="CSS3">  
<img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React">

### Backend

<img src="https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring">  
<img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot">  
<img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white" alt="Spring Security">  
<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white" alt="JWT">  
<img src="https://img.shields.io/badge/Lombok-FF0000?style=for-the-badge&logo=lombok&logoColor=white" alt="Lombok">  
<img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle">  
<img src="https://img.shields.io/badge/JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white" alt="JPA">  
<img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">  
<img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python">  
<img src="https://img.shields.io/badge/Scikit--learn-F7931E?style=for-the-badge&logo=scikit-learn&logoColor=white" alt="Scikit-learn">

### DB & Cache

<img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">  
<img src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white" alt="MongoDB">  
<img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis">  
<img src="https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" alt="Elasticsearch">

### Infra & DevOps

<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">  
<img src="https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker Compose">  
<img src="https://img.shields.io/badge/EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white" alt="EC2">  
<img src="https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black" alt="Linux">  
<img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white" alt="Grafana">  
<img src="https://img.shields.io/badge/Loki-F46800?style=for-the-badge&logo=grafana&logoColor=white" alt="Loki">  
<img src="https://img.shields.io/badge/Promtail-F46800?style=for-the-badge&logo=grafana&logoColor=white" alt="Promtail">  
<img src="https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white" alt="Jenkins">
<img src="https://img.shields.io/badge/Kibana-005571?style=for-the-badge&logo=elasticsearch&logoColor=white">

### Collaboration Tools

<img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white" alt="Notion">  
<img src="https://img.shields.io/badge/Jira-0052CC?style=for-the-badge&logo=jira&logoColor=white" alt="Jira">  
<img src="https://img.shields.io/badge/Mattermost-0058CC?style=for-the-badge&logo=mattermost&logoColor=white" alt="Mattermost">

---

## 주요 기능

1. 개인 선호도 맞춤형 매물 추천 기능  
2. 행정동 및 매물의 빅데이터 기반 점수를 통한 비교 및 GPT 요약 설명 기능  
3. 액티비티 로그 기반 협업 필터링 기능  
4. 지도, 매물 검색, 찜, 매물 및 동네 댓글 기능  
5. 네이버, 카카오 소셜 로그인

---

## Branch 별 파일 구조 및 CI/CD 파이프라인 안내

- **각 3개의 backend, frontend, recommend 브랜치** 별로 Jenkins pipeline을 통해 자동 배포 중  
- 기능 개발 시 `"feat/기능명"`의 형태로 브랜치를 생성 후 각 기능별 브랜치로 머지하여 배포  
- push 이벤트 발생 시 hook

### 1. backend

- 백엔드 서버 배포 용 브랜치  
- BE 폴더 내 Spring Boot 어플리케이션

### 2. frontend

- 프론트엔드 서버 배포 용 브랜치  
- FE 폴더 내 React 어플리케이션

### 3. recommend

- 추천 기능을 위한 파이썬 서버 배포 용 브랜치  
- ML 폴더 내 Fast API 파이썬 어플리케이션

---

## ERD

![mysql_erd.png](mysql_erd.png)

---

## 시스템 구조

![archi.png](archi.png)

---

## 화면 예시

아래는 서비스의 주요 화면들을 위한 이미지 삽입 공간과 입력 예시입니다.

### 1. 메인 화면
**설명:** 집 SEEK의 메인 페이지로, 사용자에게 서비스 소개 및 주요 기능을 보여줍니다.  

![메인 화면 예시](main_1.png)
![메인 화면 예시](main_2.png)

---

### 2. 지도 화면
**설명:** 지도 위에 매물들이 표시되어, 위치 및 주변 정보를 쉽게 확인할 수 있는 화면입니다.  

![지도 화면 예시](map_2.png)
![지도 화면 예시](map_3.png)
![지도 화면 예시](map_4.png)
![지도 화면 예시](grid_1.png)

---

### 3. 비교 화면
**설명:** 선택한 매물들의 데이터를 비교하여, 데이터 기반 의사결정을 지원하는 화면입니다.  

![비교 화면 예시](compare_1.png)
![비교 화면 예시](compare_2.png)

---

### 4. 추천 화면
**설명:** 사용자의 선호도와 데이터 분석을 기반으로 맞춤형 매물 추천 결과를 제공하는 화면입니다.  

![추천 화면 예시](recommend_1.png)
![추천 화면 예시](recommend_2.png)
![추천 화면 예시](recommend_3.png)

---

## 링크

- **사이트:** [https://j12e203.p.ssafy.io/](https://j12e203.p.ssafy.io/)
- **특화 프로젝트 공유 노션:** [https://www.notion.so/1ad3e02603998064b5b4e0ffd79cb0b2?pvs=21](https://www.notion.so/1ad3e02603998064b5b4e0ffd79cb0b2?pvs=21)
- **Figma 디자인:** [https://www.figma.com/design/LRVMjnXpOMbfpjHkvVqWkV/%ED%8A%B9%ED%99%94-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-E203(%EC%A7%91%EC%8B%9C%ED%81%AC)?node-id=0-1&t=Y1JF9QsjmRCaGPrI-1](https://www.figma.com/design/LRVMjnXpOMbfpjHkvVqWkV/%ED%8A%B9%ED%99%94-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-E203(%EC%A7%91%EC%8B%9C%ED%81%AC)?node-id=0-1&t=Y1JF9QsjmRCaGPrI-1)