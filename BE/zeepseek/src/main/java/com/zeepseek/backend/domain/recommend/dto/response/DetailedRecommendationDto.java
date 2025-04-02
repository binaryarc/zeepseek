package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.Data;

@Data
public class DetailedRecommendationDto {
    private Integer propertyId;
    private String address;
    private String roomType;
    private String contractType;
    private Integer deposit;
    private Integer monthlyRent;
    private String imageUrl;

    private Float transportScore;
    private Integer transportCount;
    private Float restaurantScore;
    private Integer restaurantCount;
    private Float healthScore;
    private Integer healthCount;
    private Float convenienceScore;
    private Integer convenienceCount;
    private Float cafeScore;
    private Integer cafeCount;
    private Float chickenScore;
    private Integer chickenCount;
    private Float leisureScore;
    private Integer leisureCount;
    private Double latitude;
    private Double longitude;

    private Double similarity;  // FastAPI 추천 결과에 포함된 유사도
}
