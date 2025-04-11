package com.zeepseek.backend.domain.recommend.dto.response;

import com.zeepseek.backend.domain.property.model.Property;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiRecommendationResponseDto {
    private List<Property> recommendedProperties = new ArrayList<>();
    private String dongName; // dongName은 null이 될 수 있음

    public AiRecommendationResponseDto() {
        this.recommendedProperties = new ArrayList<>();
    }
}
