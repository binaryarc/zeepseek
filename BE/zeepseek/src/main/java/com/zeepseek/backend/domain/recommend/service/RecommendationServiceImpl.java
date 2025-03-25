package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.RecommendationDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.exception.RecommendationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    @Value("${recommendation.api.url:http://localhost:8000}/recommend")
    private String recommendationApiUrl;

    private final WebClient webClient;

    public RecommendationServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(recommendationApiUrl).build();
    }

    @Override
    public RecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto, Pageable pageable) {
        try {
            // FastAPI 추천 API로 사용자 점수 전달 및 추천 결과 수신 (예: 상위 100개 추천)
            RecommendationResponseDto response = webClient.post()
                    .body(Mono.just(requestDto), UserRecommendationRequestDto.class)
                    .retrieve()
                    .bodyToMono(RecommendationResponseDto.class)
                    .block();

            if (response == null || response.getRecommendedProperties() == null) {
                throw new RecommendationException("No recommendations received from Python API.");
            }

            List<RecommendationDto> allRecommendations = response.getRecommendedProperties();
            int totalElements = allRecommendations.size();
            int pageSize = pageable.getPageSize();
            int currentPage = pageable.getPageNumber();
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, totalElements);
            List<RecommendationDto> pagedRecommendations = allRecommendations.subList(start, end);

            RecommendationResponseDto result = new RecommendationResponseDto();
            result.setRecommendedProperties(pagedRecommendations);
            result.setTotalElements(totalElements);
            result.setTotalPages((int) Math.ceil((double) totalElements / pageSize));
            result.setCurrentPage(currentPage);

            logger.info("Returning page {} with {} recommendations out of {} total.",
                    currentPage, pagedRecommendations.size(), totalElements);

            return result;
        } catch (Exception e) {
            logger.error("Error fetching recommendations: {}", e.getMessage(), e);
            throw new RecommendationException("Failed to fetch recommendations.", e);
        }
    }
}
