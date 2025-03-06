## 사용자 인풋 기반 추천   
- 셀레니움을 통해서 부동산 매물 데이터 추출
- 추출한 데이터를 가중치를 매겨서 전처리    
- 전처리한 데이터로 부터 특정한 동네네의 평균적인 가중치를 획득할 수 있음    
- 이걸 바탕으로 이용자 인풋과 유사한 3가지 동네 추천천   

## Collaborative Filtering(협업 필터링)    
협업 필터링(CF, Collaborative Filtering)은 크게 사용자 기반(User-Based) CF와    
아이템 기반(Item-Based) CF 방식으로 나뉘고, 이를 행렬 분해(Matrix Factorization) 및 딥러닝 방식으로 확장할 수도 있음     

예상 사용 기술: Selenium, Scikit-Learn, Surprise 라이브러리, ElasticSearch, Apache Kafka

CF 추천 시스템에서 Kafka가 하는 역할    
1️⃣ 사용자가 특정 동네 / 매물 클릭, 찜, 검색, 문의 → Kafka Producer가 이벤트 전송    
2️⃣ Kafka Broker가 로그 데이터를 저장 & 실시간 처리    
3️⃣ Kafka Consumer가 데이터를 받아 CF 추천 모델에 전달     
4️⃣ CF 추천 모델이 사용자의 유사 행동을 분석 & 추천 목록 생성    
5️⃣ 추천 결과를 Redis에 캐싱하여 빠르게 응답 제공    
6️⃣ 사용자에게 추천된 매물 or 동네를 제공    

➡ 이 과정이 실시간으로 이루어지기 때문에 사용자의 행동이 즉각적으로 추천에 반영됨    

Elasticsearch가 추천 시스템에서 사용되는 이유   
✅ 추천된 결과를 저장 & 빠르게 검색하여 실시간 응답 가능    
✅ 코사인 유사도 기반의 "비슷한 매물" 추천 최적화    
✅ Elasticsearch에서 벡터 검색을 활용해 "가장 유사한 매물"을 검색 가능    
✅ 조건(예: 가격, 위치)에 따른 필터링을 빠르게 적용 가능    
✅ Kafka + Redis와 함께 사용하면 강력한 실시간 추천 시스템 구축 가능    
