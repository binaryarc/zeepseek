package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.exception.RecommendationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);
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

        // 응답 받은 내용 로깅
        logger.info("Received recommendation response from FastAPI: {}", response);

        if (response == null || response.getRecommendedProperties() == null || response.getRecommendedProperties().isEmpty()) {
            throw new RecommendationException("No recommendations received from Python API.");
        }

        return response;
    }
}
