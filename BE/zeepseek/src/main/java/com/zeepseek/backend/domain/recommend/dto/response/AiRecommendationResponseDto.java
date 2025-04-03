package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AiRecommendationResponseDto {
    private List<DetailedRecommendationDto> recommendedProperties;
    private String dongName;
}
