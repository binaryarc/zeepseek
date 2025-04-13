package com.zeepseek.backend.domain.recommend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {

//    @JsonProperty("recommended_properties")
    private List<RecommendationDto> recommendedProperties;
    private String maxType;
}
