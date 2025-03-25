package com.zeepseek.backend.domain.recommend.dto.response;

import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import java.util.List;

public class DetailedRecommendationResponseDto {
    private List<PropertySummaryDto> recommendedProperties;
    private int totalElements;
    private int totalPages;
    private int currentPage;

    public DetailedRecommendationResponseDto() {
    }

    public DetailedRecommendationResponseDto(List<PropertySummaryDto> recommendedProperties, int totalElements, int totalPages, int currentPage) {
        this.recommendedProperties = recommendedProperties;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public List<PropertySummaryDto> getRecommendedProperties() {
        return recommendedProperties;
    }

    public void setRecommendedProperties(List<PropertySummaryDto> recommendedProperties) {
        this.recommendedProperties = recommendedProperties;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}
