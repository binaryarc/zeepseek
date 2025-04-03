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
    public void logAction(String action, String type, int userId, int age, String gender, int propertyId, int dongId) {
        // 고유 ID 생성 (예: 현재 타임스탬프 기반)
        String id = "activity_log_" + System.currentTimeMillis();

        Map<String, Object> logData = new HashMap<>();
        logData.put("userId", userId);
        logData.put("time", Instant.now().toString());
        logData.put("action", action);
        logData.put("type", type);
        logData.put("age", age);
        logData.put("gender", gender);
        logData.put("propertyId", propertyId);
        logData.put("dongId", dongId);

        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .id(id)
                    .index("logs")
                    .document(logData)
            );
            System.out.println("Indexed document, version: " + response.version());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
