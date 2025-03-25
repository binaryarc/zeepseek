package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.DetailedRecommendationResponseDto;

public interface RecommendationService {
    DetailedRecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto);
}
