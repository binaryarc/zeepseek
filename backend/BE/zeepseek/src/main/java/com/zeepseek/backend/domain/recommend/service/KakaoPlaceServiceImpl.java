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
        switch (type.toLowerCase()) {
            case "chicken":
                return Mono.fromCallable(() -> {
                    try {
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
                        Map<String, Object> out = simpleJdbcCall.execute(inParams);
                        List<PlaceInfo> resultSet = (List<PlaceInfo>) out.get("result");
                        return resultSet == null ? Collections.emptyList() : resultSet;
                    } catch (Exception e) {
                        throw new DataRetrievalException("Error retrieving chicken data", e);
                    }
                }).subscribeOn(Schedulers.boundedElastic());

            case "transport":
                return Mono.fromCallable(() -> {
                    try {
                        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                                .withProcedureName("find_transport_within_radius")
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
                        Map<String, Object> out = simpleJdbcCall.execute(inParams);
                        List<PlaceInfo> resultSet = (List<PlaceInfo>) out.get("result");
                        return resultSet == null ? Collections.emptyList() : resultSet;
                    } catch (Exception e) {
                        throw new DataRetrievalException("Error retrieving transport data", e);
                    }
                }).subscribeOn(Schedulers.boundedElastic());

            case "cafe":
            case "restaurant":
            case "health":
            case "leisure":
            case "convenience":
                String mappedCode;
                if (type.equalsIgnoreCase("cafe")) {
                    mappedCode = "CE7";
                } else if (type.equalsIgnoreCase("restaurant")) {
                    mappedCode = "FD6";
                } else if (type.equalsIgnoreCase("health")) {
                    mappedCode = "HP8";
                } else if (type.equalsIgnoreCase("leisure")) {
                    mappedCode = "AT4";
                } else { // convenience
                    mappedCode = "CS2";
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
                            if (response.getDocuments() == null) {
                                return Collections.emptyList();
                            }
                            return response.getDocuments().stream()
                                    .map(doc -> new PlaceInfo(doc.getPlaceName(), doc.getY(), doc.getX()))
                                    .collect(Collectors.toList());
                        })
                        .onErrorMap(e -> new DataRetrievalException("Error retrieving data from Kakao API", e));

            default:
                return Mono.error(new IllegalArgumentException("Unsupported category type: " + type));
        }
    }

}
