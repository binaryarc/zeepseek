package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.property.exception.PropertyNotFoundException;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.DetailedRecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.exception.RecommendationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private final WebClient recommendationWebClient;
    private final PropertyService propertyService;

    public RecommendationServiceImpl(WebClient recommendationWebClient, PropertyService propertyService) {
        this.recommendationWebClient = recommendationWebClient;
        this.propertyService = propertyService;
    }

    @Override
    public DetailedRecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto) {
        // FastAPI로부터 추천 결과 받기
        RecommendationResponseDto originalResponse = recommendationWebClient.post()
                .body(Mono.just(requestDto), UserRecommendationRequestDto.class)
                .retrieve()
                .bodyToMono(RecommendationResponseDto.class)
                .block();

        logger.info("Received recommendation response from FastAPI: {}", originalResponse);

        if (originalResponse == null || originalResponse.getRecommendedProperties() == null || originalResponse.getRecommendedProperties().isEmpty()) {
            throw new RecommendationException("No recommendations received from Python API.");
        }

        // 각 추천 항목(propertyId)로 DB에서 전체 매물 정보를 조회
        List<RecommendationDto> recList = originalResponse.getRecommendedProperties();
        List<Property> detailedProperties = new ArrayList<>();

        for (RecommendationDto rec : recList) {
            try {
                Property property = propertyService.getPropertyDetail(rec.getPropertyId());
                detailedProperties.add(property);
            } catch (PropertyNotFoundException ex) {
                logger.warn("Property not found for id: {}", rec.getPropertyId());
            }
        }

        // 페이징 정보는 필요에 따라 재구성 (여기서는 단일 페이지로 설정)
        DetailedRecommendationResponseDto detailedResponse = new DetailedRecommendationResponseDto();
        detailedResponse.setRecommendedProperties(detailedProperties);
        detailedResponse.setTotalElements(detailedProperties.size());
        detailedResponse.setTotalPages(1);
        detailedResponse.setCurrentPage(0);
        // 여기서 우선순위가 가장 높은 게 두 개 이상이라면 미리 정의해둔 우선순위로 하나 선별



        // 위에 계산해서 나온 한 카테고리를 requestDto.getMaxScoreType()을 대체
        detailedResponse.setMaxType(requestDto.getMaxScoreType());
        return detailedResponse;
    }
}