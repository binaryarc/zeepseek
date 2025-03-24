package com.zeepseek.backend.domain.recommend.controller;

import com.zeepseek.backend.domain.recommend.dto.response.RecommendationDto;
import com.zeepseek.backend.domain.recommend.dto.request.UserCategoryScoreRequestDto;
import com.zeepseek.backend.domain.recommend.service.RecommendationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommend")
public class RecommendController {

    private final RecommendationService recommendationService;

    public RecommendController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    // 추천 API: POST /api/v1/property/recommend?page=0&size=5
    // 사용자 카테고리 점수를 받아 Python FastAPI 추천 시스템과 연동하여 추천 매물 목록을 페이지네이션 처리해서 반환
    @PostMapping("/property")
    public ResponseEntity<Page<RecommendationDto>> getRecommendations(
            @RequestBody UserCategoryScoreRequestDto userCategoryScoreDto,
            Pageable pageable) {
        Page<RecommendationDto> recommendations = recommendationService.getRecommendations(userCategoryScoreDto, pageable);
        if (recommendations.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(recommendations);
    }
}
