package com.zeepseek.backend.domain.recommend.dto.response;

import com.zeepseek.backend.domain.property.model.Property;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedRecommendationResponseDto {
    private List<Property> recommendedProperties;
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private String maxType;
}
