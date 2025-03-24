package com.zeepseek.backend.domain.recommend.dto.request;

import lombok.Data;

@Data
public class UserCategoryScoreRequestDto {
    private String userIdx;
    private double transportScore;
    private double restaurantScore;
    private double healthScore;
    private double convenienceScore;
    private double cafeScore;
    private double chickenScore;
    private double leisureScore;
}
