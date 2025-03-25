package com.zeepseek.backend.domain.recommend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RecommendationResponseDto {

    @JsonProperty("recommended_properties")
    private List<Integer> recommendedProperties;

    private int totalElements;
    private int totalPages;
    private int currentPage;

    public RecommendationResponseDto() {
    }

    public RecommendationResponseDto(List<Integer> recommendedProperties, int totalElements, int totalPages, int currentPage) {
        this.recommendedProperties = recommendedProperties;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public List<Integer> getRecommendedProperties() {
        return recommendedProperties;
    }

    public void setRecommendedProperties(List<Integer> recommendedProperties) {
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
