package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.DetailedRecommendationResponseDto;
import jakarta.servlet.http.HttpServletRequest;

public interface RecommendationService {
    DetailedRecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto, HttpServletRequest request);
}
