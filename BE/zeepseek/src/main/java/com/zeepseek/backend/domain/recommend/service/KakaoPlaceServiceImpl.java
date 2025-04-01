package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.response.KakaoPlaceResponse;
import com.zeepseek.backend.domain.recommend.dto.response.PlaceInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KakaoPlaceServiceImpl implements KakaoPlaceService {

    private final WebClient webClient;
    private final JdbcTemplate jdbcTemplate;
    private static final int DEFAULT_RADIUS = 1000; // 1km = 1000미터

    public KakaoPlaceServiceImpl(@Value("${kakao.api.key}") String kakaoApiKey, JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.webClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + kakaoApiKey)
                .build();
    }

    @Override
    public Mono<List<PlaceInfo>> findPlacesWithinOneKmByType(String type, String longitude, String latitude) {
        if (type.equalsIgnoreCase("chicken")) {
            // type이 "chicken"인 경우 DB의 chicken 테이블(저장 프로시저 사용)에서 조회
            return Mono.fromCallable(() -> {
                SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                        .withProcedureName("find_chicken_within_radius");
                MapSqlParameterSource inParams = new MapSqlParameterSource()
                        .addValue("in_longitude", longitude)
                        .addValue("in_latitude", latitude)
                        .addValue("in_radius", DEFAULT_RADIUS);
                // 저장 프로시저 실행 (결과셋이 "result" 키로 반환된다고 가정)
                Map<String, Object> out = simpleJdbcCall.execute(inParams);
                List<Map<String, Object>> resultSet = (List<Map<String, Object>>) out.get("result");
                return resultSet.stream()
                        .map(row -> new PlaceInfo(
                                (String) row.get("name"),
                                row.get("latitude").toString(),
                                row.get("longitude").toString()))
                        .collect(Collectors.toList());
            }).subscribeOn(Schedulers.boundedElastic());
        } else {
            String mappedCode;
            // cafe, restaurant, medical 타입 처리
            if (type.equalsIgnoreCase("cafe") || type.equalsIgnoreCase("restaurant") || type.equalsIgnoreCase("medical")) {
                mappedCode = type.equalsIgnoreCase("cafe") ? "CE7"
                        : type.equalsIgnoreCase("restaurant") ? "FD6" : "HP8";
            }
            // transport, leisure, convenience 타입 처리
            else if (type.equalsIgnoreCase("transport") || type.equalsIgnoreCase("leisure")
                    || type.equalsIgnoreCase("convenience")) {
                mappedCode = type.equalsIgnoreCase("transport") ? "SW8"    // 교통 관련 장소
                        : type.equalsIgnoreCase("leisure") ? "AT4"         // 레저/관광 관련 장소
                        : "CS2";                                        // 편의점
            } else {
                return Mono.error(new IllegalArgumentException("Unsupported category type: " + type));
            }

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/category.json")
                            .queryParam("category_group_code", mappedCode)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", DEFAULT_RADIUS)
                            .build())
                    .retrieve()
                    .bodyToMono(KakaoPlaceResponse.class)
                    .map(response -> response.getDocuments().stream()
                            .map(doc -> new PlaceInfo(doc.getPlaceName(), doc.getY(), doc.getX()))
                            .collect(Collectors.toList())
                    );
        }
    }
}
