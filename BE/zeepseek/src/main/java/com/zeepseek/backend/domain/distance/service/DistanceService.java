package com.zeepseek.backend.domain.distance.service;

import com.zeepseek.backend.domain.distance.dto.request.CoordinateInfo;
import com.zeepseek.backend.domain.distance.dto.response.CoordinateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DistanceService {

    final double R = 6371; // 지구의 반지름 (킬로미터)

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

}
