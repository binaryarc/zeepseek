package com.zeepseek.backend.domain.distance.controller;

import com.zeepseek.backend.domain.distance.dto.request.CoordinateInfo;
import com.zeepseek.backend.domain.distance.dto.response.CoordinateResponse;
import com.zeepseek.backend.domain.distance.dto.response.KakaoTransitResponse;
import com.zeepseek.backend.domain.distance.service.DistanceService;
import com.zeepseek.backend.domain.user.entity.UserPreferences;
import com.zeepseek.backend.domain.user.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/distance")
public class DistanceController {

    private final DistanceService distanceService;
    private final UserPreferencesRepository userPreferencesRepository;

    @PostMapping("/coords")
    public ResponseEntity<CoordinateResponse> distance(@RequestBody CoordinateInfo coordinateInfo) {

        return ResponseEntity.ok(distanceService.haversineDistance(coordinateInfo));
    }

    //전희성 추가 : 시작지와 도착지 간 도보 및 대중교통 시간 호출 엔드포인트 추가
    @GetMapping("/property-transit")
    public ResponseEntity<?> getPropertyTransitInfo(
            @RequestParam Integer userId,  // 여기서 userId는 PK로 직접 사용 가능
            @RequestParam double propertyLat,
            @RequestParam double propertyLon) {

        // 사용자 선호도 정보 조회 (userId가 PK이므로 findById 바로 사용)
        UserPreferences userPreferences = userPreferencesRepository.findById(userId)
                .orElse(null);

        if (userPreferences == null ||
                userPreferences.getLatitude() == null ||
                userPreferences.getLongitude() == null) {
            return ResponseEntity.badRequest().body("User destination not set");
        }

        // 카카오 API 호출
        KakaoTransitResponse response = distanceService.getKakaoTransitInfo(
                propertyLat,
                propertyLon,
                userPreferences.getLatitude(),
                userPreferences.getLongitude());

        return ResponseEntity.ok(response);
    }
}
