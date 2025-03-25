package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.Data;
import com.zeepseek.backend.domain.recommend.dto.RecommendationDto;
import java.util.List;

@Data
public class RecommendationResponseDto {
    private List<RecommendationDto> recommendedProperties;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
