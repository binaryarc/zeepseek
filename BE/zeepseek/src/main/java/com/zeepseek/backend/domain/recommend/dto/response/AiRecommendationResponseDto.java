package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiRecommendationResponseDto {
    private List<DetailedRecommendationDto> recommendedProperties = new ArrayList<>();
    private String dongName; // dongName은 null이 될 수 있음

    public AiRecommendationResponseDto() {
        this.recommendedProperties = new ArrayList<>();
    }
}
