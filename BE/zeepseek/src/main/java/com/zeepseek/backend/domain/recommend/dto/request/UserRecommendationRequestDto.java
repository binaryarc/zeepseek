package com.zeepseek.backend.domain.recommend.dto.request;

import lombok.Data;

@Data
public class UserRecommendationRequestDto {
    private Long userId;
    private Double transportScore;
    private Double restaurantScore;
    private Double healthScore;
    private Double convenienceScore;
    private Double cafeScore;
    private Double chickenScore;
    private Double leisureScore;
}
