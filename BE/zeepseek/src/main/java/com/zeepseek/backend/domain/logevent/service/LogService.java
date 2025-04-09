package com.zeepseek.backend.domain.logevent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class LogService {

    private final ElasticsearchClient elasticsearchClient;

    @Async
    public void logAction(String action, String type, int userId, int age, String gender, int propertyId, int dongId) {
        // 고유 ID 생성 (예: 현재 타임스탬프 기반)
        String id = "activity_log_" + System.currentTimeMillis() + "_" + new Random().nextInt(10000);

        Map<String, Object> logData = new HashMap<>();
        logData.put("userId", userId);
        logData.put("time", Instant.now().toString());
        logData.put("action", action);
        logData.put("type", type);
        logData.put("age", age);
        logData.put("gender", gender);
        logData.put("propertyId", propertyId);
        logData.put("dongId", dongId);

        // 추가: properties 인덱스에서 propertyId에 해당하는 문서를 조회
        try {
            // propertyId를 _id로 사용하여 properties 인덱스에서 해당 문서를 가져옵니다.
            GetResponse<Map<String, Object>> propertyResponse = elasticsearchClient.get(g -> g
                            .index("properties")
                            .id(String.valueOf(propertyId))
                    , (Class<Map<String, Object>>)(Class<?>) Map.class);

            if (propertyResponse.found()) {
                Map<String, Object> propertyData = propertyResponse.source();
                // 예를 들어, properties 인덱스에 "specificField"라는 필드가 있다고 가정합시다.
                if (propertyData != null && propertyData.containsKey("computedRoomType")) {
                    logData.put("computedRoomType", propertyData.get("computedRoomType"));
                }
                if (propertyData != null && propertyData.containsKey("roomType")) {
                    logData.put("roomType", propertyData.get("roomType"));
                }
                if (propertyData != null && propertyData.containsKey("contractType")) {
                    logData.put("contractType", propertyData.get("contractType"));
                }
            } else {
                System.out.println("properties 인덱스에 propertyId " + propertyId + "에 해당하는 문서가 없습니다.");
            }
        } catch (Exception e) {
            // 조회 실패 시 예외 처리 (필요에 따라 기본값 설정 가능)
            e.printStackTrace();
        }

        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .id(id)
                    .index("logs")
                    .document(logData)
            );
            System.out.println("Indexed document, version: " + response.version() + " " + dongId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
