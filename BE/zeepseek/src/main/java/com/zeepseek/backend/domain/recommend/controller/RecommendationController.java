package com.zeepseek.backend.domain.recommend.controller;

import com.zeepseek.backend.domain.auth.util.CookieUtils;
import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.AiRecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.dto.response.DetailedRecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Autowired
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<DetailedRecommendationResponseDto> getRecommendations(
            @RequestBody UserRecommendationRequestDto requestDto,
            HttpServletRequest request) {

        // 쿠키에서 사용자 활동 정보(나이, 성별) 가져오기
        Optional<Map<String, Integer>> userActivityInfo = CookieUtils.getUserActivityInfoFromCookie(request);

        // 활동 정보가 존재하면 DTO에 설정
        userActivityInfo.ifPresent(info -> {
            requestDto.setAge(info.get("age"));
            requestDto.setGender(info.get("gender"));
        });

        // 캐싱된 추천 결과에 대해 후처리(찜 정보 업데이트) 후 반환
        DetailedRecommendationResponseDto responseDto = recommendationService.getRecommendationsWithUpdatedLikes(requestDto, request);
        return ResponseEntity.ok(responseDto);
    }
    // AI 기반 추천 (GET /api/ai-recommend?user_id=123)
    @GetMapping("/ai-recommend")
    public ResponseEntity<AiRecommendationResponseDto> getAiRecommendation(@RequestParam("userId") Integer userId) {
        AiRecommendationResponseDto responseDto = recommendationService.getAiRecommendation(userId);
        return ResponseEntity.ok(responseDto);
    }
}