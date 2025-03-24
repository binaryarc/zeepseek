package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class RecommendationResponseDto {
    private List<RecommendationDto> recommendedProperties;
}
