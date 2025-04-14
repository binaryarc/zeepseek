package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.AiRecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.dto.response.DetailedRecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import jakarta.servlet.http.HttpServletRequest;

public interface RecommendationService {

    AiRecommendationResponseDto getAiRecommendation(Integer userId);

    DetailedRecommendationResponseDto getRecommendationsWithUpdatedLikes(UserRecommendationRequestDto requestDto, HttpServletRequest request);
}
