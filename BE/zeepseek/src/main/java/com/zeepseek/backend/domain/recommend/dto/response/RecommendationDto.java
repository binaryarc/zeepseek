package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {
    private Long propertyId;
    private double similarity;
    private String primaryCategory;
}
