package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.property.exception.PropertyNotFoundException;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.model.PropertyScore;
import com.zeepseek.backend.domain.property.service.PropertyServiceImpl;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.DetailedRecommendationDto;
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
        RecommendationResponseDto originalResponse = recommendationWebClient.post()
                .body(Mono.just(requestDto), UserRecommendationRequestDto.class)
                .retrieve()
                .bodyToMono(RecommendationResponseDto.class)
                .block();

        if (originalResponse == null || originalResponse.getRecommendedProperties() == null || originalResponse.getRecommendedProperties().isEmpty()) {
            throw new RecommendationException("No recommendations received from Python API.");
        }

        List<RecommendationDto> recList = originalResponse.getRecommendedProperties();
        List<DetailedRecommendationDto> detailedList = new ArrayList<>();

        for (RecommendationDto rec : recList) {
            try {
                Property property = propertyService.getPropertyDetail(rec.getPropertyId());
                PropertyScore score = propertyService.getPropertyScoreByPropertyId(Math.toIntExact(rec.getPropertyId()));

                DetailedRecommendationDto dto = new DetailedRecommendationDto();
                dto.setPropertyId(property.getPropertyId());
                dto.setAddress(property.getAddress());
                dto.setRoomType(property.getRoomType());
                dto.setContractType(property.getContractType());
                dto.setDeposit(property.getDeposit());
                dto.setMonthlyRent(property.getMonthlyRent());
                dto.setImageUrl(property.getImageUrl());

                dto.setTransportScore(score.getTransportScore());
                dto.setTransportCount(score.getTransportCount());
                dto.setRestaurantScore(score.getRestaurantScore());
                dto.setRestaurantCount(score.getRestaurantCount());
                dto.setHealthScore(score.getHealthScore());
                dto.setHealthCount(score.getHealthCount());
                dto.setConvenienceScore(score.getConvenienceScore());
                dto.setConvenienceCount(score.getConvenienceCount());
                dto.setCafeScore(score.getCafeScore());
                dto.setCafeCount(score.getCafeCount());
                dto.setChickenScore(score.getChickenScore());
                dto.setChickenCount(score.getChickenCount());
                dto.setLeisureScore(score.getLeisureScore());
                dto.setLeisureCount(score.getLeisureCount());

                dto.setSimilarity(rec.getSimilarity());  // 추천 유사도 세팅

                detailedList.add(dto);
            } catch (PropertyNotFoundException ex) {
                logger.warn("Property not found for id: {}", rec.getPropertyId());
            }
        }

        DetailedRecommendationResponseDto detailedResponse = new DetailedRecommendationResponseDto();
        detailedResponse.setRecommendedProperties(detailedList);
        detailedResponse.setTotalElements(detailedList.size());
        detailedResponse.setTotalPages(1);
        detailedResponse.setCurrentPage(0);
        // 여기서 우선순위가 가장 높은 게 두 개 이상이라면 미리 정의해둔 우선순위로 하나 선별



        // 위에 계산해서 나온 한 카테고리를 requestDto.getMaxScoreType()을 대체
        detailedResponse.setMaxType(requestDto.getMaxScoreType());

        return detailedResponse;
    }
}