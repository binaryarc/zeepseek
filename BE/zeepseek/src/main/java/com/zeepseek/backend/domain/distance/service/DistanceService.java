package com.zeepseek.backend.domain.distance.service;

import com.zeepseek.backend.domain.distance.dto.request.CoordinateInfo;
import com.zeepseek.backend.domain.distance.dto.response.CoordinateResponse;

import com.zeepseek.backend.domain.distance.dto.response.KakaoTransitResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DistanceService {

    final double R = 6371; // 지구의 반지름 (킬로미터)

    // 전희성 추가 : RestAPI 및 webclient 추가 시작
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoApiKey;

    private final WebClient mobilityWebClient;
    private final WebClient localWebClient;

    public DistanceService(WebClient.Builder webClientBuilder) {
        this.mobilityWebClient = webClientBuilder.baseUrl("https://apis-navi.kakaomobility.com").build();
        this.localWebClient = webClientBuilder.baseUrl("https://dapi.kakao.com").build();
    }
    // 전희성 추가 : RestAPI 및 webclient 추가 끝

    public CoordinateResponse haversineDistance (CoordinateInfo coordinateInfo) {

        double distance = haversine(coordinateInfo.getLat1(),
                coordinateInfo.getLon1(),
                coordinateInfo.getLat2(),
                coordinateInfo.getLon2());
        log.info("distance: {}", distance);
        int result = calculateWalkingTime(distance);
        log.info("최종 경과 예상 시간 (5km): {}", result);
        return CoordinateResponse.builder().second(result).build();
    }

    // 하버사인 공식을 이용해 두 지점 간의 거리를 계산하는 메서드 (단위: km)
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반지름 (킬로미터)

        // 위도와 경도의 차이를 라디안 단위로 변환
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // 하버사인 공식 적용
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 최종 거리 계산
        return R * c;
    }

    // 걷는 속도(5 km/h)를 기준으로 걸리는 시간을 분 단위로 계산하는 메서드
    public static int calculateWalkingTime(double distanceKm) {
        double walkingSpeed = 5.0; // km/h
        double timeHours = distanceKm / walkingSpeed;

        return (int) Math.round(timeHours * 3600);
    }

    //전희성 추가 : 카카오 API를 이용해 도보 및 대중교통 시간 추출 시작
    // 카카오 API를 이용한 도보/대중교통 시간 조회 메서드
    public KakaoTransitResponse getKakaoTransitInfo(double lat1, double lon1, double lat2, double lon2) {
        log.info("카카오 API 호출 - 도보/대중교통 시간 조회");
        log.info("출발지: {}, {}", lat1, lon1);
        log.info("도착지: {}, {}", lat2, lon2);

        // 도보 API 호출 - 카카오 로컬 API로 변경
        Map<String, Object> walkResponse = callKakaoLocalAPI(lon1, lat1, lon2, lat2, "pedestrian");
        log.info("도보 API 응답: {}", walkResponse);

        // 대중교통 API 호출 - 카카오 로컬 API로 변경
        Map<String, Object> transitResponse = callKakaoLocalAPI(lon1, lat1, lon2, lat2, "transit");
        log.info("대중교통 API 응답: {}", transitResponse);

        // 자동차 API 호출 - 기존 모빌리티 API 유지
        Map<String, Object> drivingResponse = callKakaoMobilityAPI(lon1, lat1, lon2, lat2, "DRIVING");
        log.info("자동차 API 응답: {}", drivingResponse);

        // 응답 파싱
        Integer walkingDuration = extractLocalDuration(walkResponse);
        Integer transitDuration = extractLocalDuration(transitResponse);
        Integer drivingDuration = extractMobilityDuration(drivingResponse);

        // 도보 시간이 null인 경우 하버사인 공식 사용
        if (walkingDuration == null) {
            double distance = haversine(lat1, lon1, lat2, lon2);
            walkingDuration = calculateWalkingTime(distance);
            log.info("도보 시간 API 획득 실패, 하버사인 공식 사용: {}초", walkingDuration);
        }

        KakaoTransitResponse response = KakaoTransitResponse.builder()
                .walkingDuration(walkingDuration)
                .transitDuration(transitDuration)
                .drivingDuration(drivingDuration)
                .build();

        log.info("최종 응답: {}", response);
        return response;
    }

    // 카카오 로컬 API 호출 메서드 (도보, 대중교통용)
    private Map<String, Object> callKakaoLocalAPI(double sLon, double sLat, double eLon, double eLat, String type) {
        try {
            return localWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/directions/" + type) // pedestrian 또는 transit
                            .queryParam("origin", sLon + "," + sLat)
                            .queryParam("destination", eLon + "," + eLat)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        log.error("카카오 로컬 API 오류: {}", error);
                                        return Mono.error(new RuntimeException("API 호출 실패: " + error));
                                    })
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .onErrorResume(e -> {
                        log.error("로컬 API 호출 중 오류 발생: {}", e.getMessage());
                        return Mono.just(new HashMap<>());
                    })
                    .block();
        } catch (Exception e) {
            log.error("로컬 API 호출 예외: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    // 카카오모빌리티 API 호출 메서드 (자동차용)
    private Map<String, Object> callKakaoMobilityAPI(double sLon, double sLat, double eLon, double eLat, String mode) {
        try {
            return mobilityWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/directions")
                            .queryParam("origin", sLon + "," + sLat)
                            .queryParam("destination", eLon + "," + eLat)
                            .queryParam("priority", "TIME")
                            .queryParam("car_type", "1")  // 자동차 유형 (1: 일반)
                            .queryParam("mode", mode)     // 이동 수단 (DRIVING)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        log.error("카카오모빌리티 API 오류: {}", error);
                                        return Mono.error(new RuntimeException("API 호출 실패: " + error));
                                    })
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .onErrorResume(e -> {
                        log.error("모빌리티 API 호출 중 오류 발생: {}", e.getMessage());
                        return Mono.just(new HashMap<>());
                    })
                    .block();
        } catch (Exception e) {
            log.error("모빌리티 API 호출 예외: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    // 카카오 로컬 API 응답에서 소요 시간 추출
    @SuppressWarnings("unchecked")
    private Integer extractLocalDuration(Map<String, Object> response) {
        try {
            // 로컬 API는 응답 구조가 다를 수 있음
            if (response != null) {
                if (response.containsKey("routes")) {
                    List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                    if (!routes.isEmpty()) {
                        Map<String, Object> route = routes.get(0);
                        if (route.containsKey("duration")) {
                            return (Integer) route.get("duration");
                        }
                    }
                } else if (response.containsKey("duration")) {
                    // 일부 API는 최상위에 duration이 있을 수 있음
                    return (Integer) response.get("duration");
                }
            }
        } catch (Exception e) {
            log.error("카카오 로컬 API 응답 파싱 오류", e);
        }
        return null;
    }

    // 카카오 모빌리티 API 응답에서 소요 시간 추출 (기존 메서드)
    @SuppressWarnings("unchecked")
    private Integer extractMobilityDuration(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);

                    // 소요 시간 (초)
                    if (route.containsKey("duration")) {
                        return (Integer) route.get("duration");
                    }

                    // 혹은 summary에 있을 수도 있음
                    if (route.containsKey("summary")) {
                        Map<String, Object> summary = (Map<String, Object>) route.get("summary");
                        if (summary.containsKey("duration")) {
                            return (Integer) summary.get("duration");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("카카오 모빌리티 API 응답 파싱 오류", e);
        }
        return null;
    }
    //전희성 추가 : 카카오 API를 이용해 도보 및 대중교통 시간 추출 끝
}