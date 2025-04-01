package com.zeepseek.backend.domain.logevent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogService {

    private final ElasticsearchClient elasticsearchClient;

    @Async
    public void logAction(String action,int userId, int age, String gender, String type, int propertyId, int dongId) {
        // 고유 ID를 생성합니다. (예시로 현재 타임스탬프 사용)
        String id = "activity_log_" + System.currentTimeMillis();

        // 로그 데이터를 구성합니다.
        Map<String, Object> logData = new HashMap<>();
        logData.put("userId", userId);
        logData.put("time", Instant.now().toString());
        logData.put("action", action);
        // 기타 필드 (예시)
        logData.put("age", age);
        logData.put("gender", gender);
        logData.put("type", type);
        logData.put("propertyId", propertyId);
        logData.put("dongId", dongId);

        try {
            // Elasticsearch의 "logs" 인덱스에 비동기 저장 (엘라스틱서치 클라이언트는 내부적으로 비동기 처리도 지원합니다)
            IndexResponse response = elasticsearchClient.index(i -> i
                    .id(id)
                    .index("logs")
                    .document(logData)
            );
            // 응답 처리: 필요한 경우 로그 출력 등 추가 작업
            System.out.println("Indexed document, version: " + response.version());
        } catch (Exception e) {
            // 예외 처리: 로깅 혹은 재시도 로직 구현
            e.printStackTrace();
        }
    }
}
