package com.zeepseek.backend.domain.recommend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import com.zeepseek.backend.domain.recommend.dto.RecommendationDto;
import java.util.List;

@Data
public class RecommendationResponseDto {
    @JsonProperty("recommended_properties")
    private List<Integer> recommendedProperties;
    private int totalElements;
    private int totalPages;
    private int currentPage;

    // getters, setters, constructors 생략
}