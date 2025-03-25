package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.exception.RecommendationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final WebClient recommendationWebClient;

    public RecommendationServiceImpl(WebClient recommendationWebClient) {
        this.recommendationWebClient = recommendationWebClient;
    }

    @Override
    public RecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto) {
        RecommendationResponseDto response = recommendationWebClient.post()
                .body(Mono.just(requestDto), UserRecommendationRequestDto.class)
                .retrieve()
                .bodyToMono(RecommendationResponseDto.class)
                .block();

        if (response == null || response.getRecommendedProperties() == null || response.getRecommendedProperties().isEmpty()) {
            throw new RecommendationException("No recommendations received from Python API.");
        }

        return response;
    }
}
