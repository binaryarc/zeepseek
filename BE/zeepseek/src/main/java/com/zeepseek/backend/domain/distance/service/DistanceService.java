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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DistanceService {

    final double R = 6371; // 지구의 반지름 (킬로미터)

    // 전희성 추가 : RestAPI 및 webclient 추가 시작
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoApiKey;

    private final WebClient webClient;
    public DistanceService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://dapi.kakao.com").build();
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

        // 도보 API 호출
        Map<String, Object> walkResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/directions/walk")
                        .queryParam("origin", lon1 + "," + lat1)         // 경도,위도 순서
                        .queryParam("destination", lon2 + "," + lat2)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        log.info("도보 API 응답: {}", walkResponse);

        // 대중교통 API 호출
        Map<String, Object> transitResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/directions/transit")
                        .queryParam("origin", lon1 + "," + lat1)         // 경도,위도 순서
                        .queryParam("destination", lon2 + "," + lat2)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        log.info("대중교통 API 응답: {}", transitResponse);

        // 응답 파싱
        Integer walkingDuration = extractDuration(walkResponse);
        Integer transitDuration = extractDuration(transitResponse);

        KakaoTransitResponse response = KakaoTransitResponse.builder()
                .walkingDuration(walkingDuration)
                .transitDuration(transitDuration)
                .build();

        log.info("최종 응답: {}", response);
        return response;
    }

    // 카카오 API 응답에서 소요 시간 추출
    @SuppressWarnings("unchecked")
    private Integer extractDuration(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    return (Integer) routes.get(0).get("duration");
                }
            }
        } catch (Exception e) {
            log.error("카카오 API 응답 파싱 오류", e);
        }
        return null;
    }
    //전희성 추가 : 카카오 API를 이용해 도보 및 대중교통 시간 추출 끝
}
