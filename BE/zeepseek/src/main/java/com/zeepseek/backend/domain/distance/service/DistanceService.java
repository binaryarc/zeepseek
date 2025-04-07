package com.zeepseek.backend.domain.distance.service;

import com.zeepseek.backend.domain.distance.dto.request.CoordinateInfo;
import com.zeepseek.backend.domain.distance.dto.response.CoordinateResponse;
import com.zeepseek.backend.domain.distance.dto.response.TransitResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DistanceService {

    final double R = 6371; // 지구의 반지름 (킬로미터)

    // 전희성 추가 : RestAPI 및 webclient 추가 시작

    // 생성자 주입 방식 사용 (추천 방법: 1번)
    private final String kakaoApiKey;
    private final String tmapApiKey;

    private final WebClient mobilityWebClient;
    private final WebClient tmapWebClient;

    public DistanceService(WebClient.Builder webClientBuilder,
                           @Value("${spring.security.oauth2.client.registration.kakao.client-id}") String kakaoApiKey,
                           @Value("${tmap.api.key}") String tmapApiKey) {
        this.kakaoApiKey = kakaoApiKey;
        this.tmapApiKey = tmapApiKey;

        HttpClient httpClient = HttpClient.create().protocol(HttpProtocol.HTTP11);
        this.mobilityWebClient = webClientBuilder
                .baseUrl("https://apis-navi.kakaomobility.com")
                .build();
        this.tmapWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://apis.openapi.sk.com")
                .defaultHeader("appKey", tmapApiKey)   // 기본 헤더로 appKey 세팅
                .build();
    }
    // 전희성 추가 : RestAPI 및 webclient 추가 끝

    // 티맵 API 로그 추가
    @PostConstruct
    public void init() {
        // 디버깅을 위한 로그 출력 (프로덕션에서는 제거 권장)
        log.info("TMap API Key: {}", tmapApiKey);
    }

    public CoordinateResponse haversineDistance(CoordinateInfo coordinateInfo) {

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
    public TransitResponse getTransitInfo(double lat1, double lon1, double lat2, double lon2) {
        log.info("이동 시간 정보 조회");
        log.info("출발지: {}, {}", lat1, lon1);
        log.info("도착지: {}, {}", lat2, lon2);

        // 도보 시간 - TMap API 사용
        Integer walkingDuration = getTmapPedestrianDuration(lat1, lon1, lat2, lon2);
        log.info("TMap 도보 시간: {}초", walkingDuration);

        // 대중교통 시간 - ODsay API 사용
        Integer transitDuration = getTmapTransitDuration(lat1, lon1, lat2, lon2);
        log.info("TMap 대중교통 시간: {}초", transitDuration);

        // 자동차 API 호출 - 기존 모빌리티 API 유지
        Integer drivingDuration = getKakaoMobilityDuration(lon1, lat1, lon2, lat2);
        log.info("카카오 자동차 시간: {}초", drivingDuration);

        // 도보 시간이 null인 경우 하버사인 공식 사용
        if (walkingDuration == null) {
            double distance = haversine(lat1, lon1, lat2, lon2);
            walkingDuration = calculateWalkingTime(distance);
            log.info("도보 시간 API 획득 실패, 하버사인 공식 사용: {}초", walkingDuration);
        }

        TransitResponse response = TransitResponse.builder()
                .walkingDuration(walkingDuration)
                .transitDuration(transitDuration)
                .drivingDuration(drivingDuration)
                .build();

        log.info("최종 응답: {}", response);
        return response;
    }

    // TMap 보행자 경로 API 호출 메서드 추가
    private Integer getTmapPedestrianDuration(double startLat, double startLon, double endLat, double endLon) {
        try {
            // API 요청 본문 구성
            Map<String, Object> requestBody = new HashMap<>();

            // 출발지 좌표
            requestBody.put("startX", startLon);  // 경도(Longitude)
            requestBody.put("startY", startLat);  // 위도(Latitude)
            requestBody.put("startName", "출발지");

            // 도착지 좌표
            requestBody.put("endX", endLon);  // 경도(Longitude)
            requestBody.put("endY", endLat);  // 위도(Latitude)
            requestBody.put("endName", "도착지");

            // 옵션 설정 - 최단시간
            requestBody.put("searchOption", "0");

            Map<String, Object> response = tmapWebClient.post()
                    .uri("/tmap/routes/pedestrian?version=1&format=json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .onErrorResume(e -> {
                        log.error("TMap API 호출 중 오류 발생: {}", e.getMessage());
                        return Mono.just(new HashMap<>());
                    })
                    .block();

            return extractTmapPedestrianDuration(response);
        } catch (Exception e) {
            log.error("TMap API 호출 예외: {}", e.getMessage());
            return null;
        }
    }

    // TMap API 응답에서 소요 시간 추출 메서드 추가
    @SuppressWarnings("unchecked")
    private Integer extractTmapPedestrianDuration(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("features")) {
                List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");
                for (Map<String, Object> feature : features) {
                    if (feature.containsKey("properties")) {
                        Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
                        if (properties.containsKey("totalTime")) {
                            // TMap은 분 단위로 제공, 초 단위로 변환
                            int totalTimeSec = Integer.parseInt(properties.get("totalTime").toString());
                            // 실제 응답은 초 단위이므로 변환 불필요 (기존 주석 유지용)
                            return totalTimeSec;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("TMap API 응답 파싱 오류", e);
        }
        return null;
    }

    // TMap 대중교통 추가
    private Integer getTmapTransitDuration(double startLat, double startLon, double endLat, double endLon) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("startX", startLon);
            body.put("startY", startLat);
            body.put("endX", endLon);
            body.put("endY", endLat);
            body.put("count", 1);
            body.put("lang", 0);
            body.put("format", "json");

            Map<String, Object> response = tmapWebClient.post()
                    .uri("/transit/routes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .onErrorResume(e -> Mono.just(new HashMap<>()))
                    .block();

            return extractTmapTransitDuration(response);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Integer extractTmapTransitDuration(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("metaData")) {
                Map<String, Object> metaData = (Map<String, Object>) response.get("metaData");
                Map<String, Object> plan = (Map<String, Object>) metaData.get("plan");
                List<Map<String, Object>> itineraries = (List<Map<String, Object>>) plan.get("itineraries");
                if (itineraries != null && !itineraries.isEmpty()) {
                    Map<String, Object> first = itineraries.get(0);
                    if (first.containsKey("totalTime")) {
                        return Integer.parseInt(first.get("totalTime").toString());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // 카카오모빌리티 API를 이용한 자동차 소요 시간만 반환
    private Integer getKakaoMobilityDuration(double sLon, double sLat, double eLon, double eLat) {
        try {
            Map<String, Object> mobilityResponse = mobilityWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/directions")
                            .queryParam("origin", sLon + "," + sLat)
                            .queryParam("destination", eLon + "," + eLat)
                            .queryParam("priority", "TIME")
                            .queryParam("car_type", "1")  // 자동차 유형 (1: 일반)
                            .queryParam("mode", "DRIVING")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
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

            return extractMobilityDuration(mobilityResponse);
        } catch (Exception e) {
            log.error("모빌리티 API 호출 예외: {}", e.getMessage());
            return null;
        }
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
