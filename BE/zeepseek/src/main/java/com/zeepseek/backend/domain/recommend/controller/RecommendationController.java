package com.zeepseek.backend.domain.recommend.controller;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Autowired
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<RecommendationResponseDto> getRecommendations(
            @RequestBody UserRecommendationRequestDto requestDto) {
        RecommendationResponseDto responseDto = recommendationService.getRecommendations(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
