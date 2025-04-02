package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.response.KakaoPlaceResponse;
import com.zeepseek.backend.domain.recommend.dto.response.PlaceInfo;
import com.zeepseek.backend.domain.recommend.exception.DataRetrievalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
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
    public Mono<List<?>> findPlacesWithinOneKmByType(String type, String longitude, String latitude) {
        if (type.equalsIgnoreCase("chicken")) {
            // type이 "chicken"인 경우 DB의 chicken 테이블(저장 프로시저 사용)에서 조회
            return Mono.fromCallable(() -> {
                try {
                    // 저장 프로시저 결과를 PlaceInfo 객체로 매핑하도록 설정
                    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                            .withProcedureName("find_chicken_within_radius")
                            .returningResultSet("result", (rs, rowNum) ->
                                    new PlaceInfo(
                                            rs.getString("name"),
                                            rs.getString("latitude"),
                                            rs.getString("longitude")
                                    )
                            );
                    MapSqlParameterSource inParams = new MapSqlParameterSource()
                            .addValue("in_longitude", longitude)
                            .addValue("in_latitude", latitude)
                            .addValue("in_radius", DEFAULT_RADIUS);

                    // 저장 프로시저 실행
                    Map<String, Object> out = simpleJdbcCall.execute(inParams);
                    List<PlaceInfo> resultSet = (List<PlaceInfo>) out.get("result");
                    // 결과셋이 null이면 빈 리스트 반환
                    return resultSet == null ? Collections.emptyList() : resultSet;
                } catch (Exception e) {
                    throw new DataRetrievalException("Error retrieving chicken data", e);
                }
            }).subscribeOn(Schedulers.boundedElastic());
        } else {
            String mappedCode;
            // cafe, restaurant, medical 타입 처리
            if (type.equalsIgnoreCase("cafe") || type.equalsIgnoreCase("restaurant") || type.equalsIgnoreCase("health")) {
                mappedCode = type.equalsIgnoreCase("cafe") ? "CE7"
                        : type.equalsIgnoreCase("restaurant") ? "FD6" : "HP8";
            }
            // transport, leisure, convenience 타입 처리
            else if (type.equalsIgnoreCase("transport") || type.equalsIgnoreCase("leisure")
                    || type.equalsIgnoreCase("convenience") ) {
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
                    .map(response -> {
                        // documents가 null일 경우 빈 리스트를 반환
                        if (response.getDocuments() == null) {
                            return Collections.emptyList();
                        }
                        return response.getDocuments().stream()
                                .map(doc -> new PlaceInfo(doc.getPlaceName(), doc.getY(), doc.getX()))
                                .collect(Collectors.toList());
                    })
                    .onErrorMap(e -> new DataRetrievalException("Error retrieving data from Kakao API", e));

        }
    }
}
