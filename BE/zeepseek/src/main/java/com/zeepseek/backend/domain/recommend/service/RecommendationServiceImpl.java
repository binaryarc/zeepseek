package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.RecommendationDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.exception.RecommendationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final String recommendationApiUrl;
    private final WebClient webClient;

    public RecommendationServiceImpl(WebClient.Builder webClientBuilder,
                                     @Value("${recommendation.api.url:http://recommend_container:8000/recommend}") String recommendationApiUrl) {
        this.recommendationApiUrl = recommendationApiUrl;
        this.webClient = webClientBuilder
                .baseUrl(this.recommendationApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();

        logger.info("Recommendation API URL configured: {}", this.recommendationApiUrl);
    }

    @Override
    public RecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto, Pageable pageable) {
        try {
            // Debug: Log the request DTO before sending
            logger.debug("Request DTO being sent: {}", requestDto);

            RecommendationResponseDto response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(requestDto), UserRecommendationRequestDto.class)
                    .retrieve()
                    .onStatus(
                            httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        logger.error("Error response from recommendation API: Status {}, Body: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return Mono.error(new RecommendationException("API Error: " + errorBody));
                                    })
                    )
                    .bodyToMono(RecommendationResponseDto.class)
                    .block();

            if (response == null || response.getRecommendedProperties() == null) {
                throw new RecommendationException("No recommendations received from Python API.");
            }

            // Rest of the method remains the same as your original implementation
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
        } catch (WebClientResponseException e) {
            // More detailed logging for WebClient specific exceptions
            logger.error("WebClient Error - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RecommendationException("Failed to fetch recommendations: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("Error fetching recommendations: {}", e.getMessage(), e);
            throw new RecommendationException("Failed to fetch recommendations.", e);
        }
    }
}