package com.zeepseek.backend.domain.recommend.dto;

import lombok.Data;

@Data
public class RecommendationDto {
    private Long propertyId;
    private Double similarity;
}
