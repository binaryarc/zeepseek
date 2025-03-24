package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserCategoryScoreRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.exception.RecommendationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    // Python FastAPI 추천 API URL (내부 네트워크 IP 또는 도메인)
    @Value("${recommendation.api.url:http://localhost:8000/recommend}")
    private String recommendationApiUrl;

    private final WebClient webClient;

    public RecommendationServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(recommendationApiUrl).build();
    }

    @Override
    public Page<RecommendationDto> getRecommendations(UserCategoryScoreRequestDto userCategoryScoreDto, Pageable pageable) {
        try {
            RecommendationResponseDto response = webClient.post()
                    .body(Mono.just(userCategoryScoreDto), UserCategoryScoreRequestDto.class)
                    .retrieve()
                    .bodyToMono(RecommendationResponseDto.class)
                    .block();

            if (response == null || response.getRecommendedProperties() == null) {
                logger.warn("No recommendations received from Python API for user score: {}", userCategoryScoreDto);
                throw new RecommendationException("NO_RECOMMENDATION", "No recommendations received from Python API.", null);
            }

            List<RecommendationDto> recommendations = response.getRecommendedProperties();
            int totalElements = recommendations.size();

            // 페이지네이션 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), totalElements);
            List<RecommendationDto> pageList = recommendations.subList(start, end);

            logger.info("Returning {} recommendations out of {} total. Page: {}", pageList.size(), totalElements, pageable.getPageNumber());
            return new PageImpl<>(pageList, pageable, totalElements);
        } catch (WebClientResponseException e) {
            logger.error("WebClient error fetching recommendations: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new RecommendationException("WEBCLIENT_ERROR", "Error fetching recommendations: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error fetching recommendations: {}", e.getMessage(), e);
            throw new RecommendationException("Failed to fetch recommendations.", e);
        }
    }
}
