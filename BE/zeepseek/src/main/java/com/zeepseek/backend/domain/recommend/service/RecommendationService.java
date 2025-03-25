package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;

public interface RecommendationService {
    RecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto);
}