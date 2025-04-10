package com.zeepseek.backend.domain.recommend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeepseek.backend.domain.property.exception.PropertyNotFoundException;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.model.PropertyScore;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.response.*;
import com.zeepseek.backend.domain.recommend.exception.RecommendationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RecommendationCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private final WebClient recommendationWebClient;
    private final PropertyService propertyService;
    public RecommendationCacheService(WebClient recommendationWebClient, PropertyService propertyService) {
        this.recommendationWebClient = recommendationWebClient;
        this.propertyService = propertyService;
    }

    @Cacheable(value = "recommendations", key = "#requestDto.getCacheKey()", unless = "#result == null", cacheManager = "propertyCacheManager")
    public DetailedRecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto, HttpServletRequest request) {
        // 1) 쿠키에서 age, gender, userId 정보 추출 (생략 없이 기존 로직 사용)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("age".equals(cookie.getName())) {
                    try {
                        requestDto.setAge(Integer.parseInt(cookie.getValue()));
                        logger.info("사용자 나이 설정: {}", cookie.getValue());
                    } catch (NumberFormatException e) {
                        logger.warn("쿠키에서 나이 파싱 실패: {}", cookie.getValue());
                    }
                } else if ("gender".equals(cookie.getName())) {
                    try {
                        requestDto.setGender(Integer.parseInt(cookie.getValue()));
                        logger.info("사용자 성별 설정: {}", cookie.getValue());
                    } catch (NumberFormatException e) {
                        logger.warn("쿠키에서 성별 파싱 실패: {}", cookie.getValue());
                    }
                } else if ("userId".equals(cookie.getName())) {
                    try {
                        requestDto.setUserId(Long.parseLong(cookie.getValue()));
                        logger.info("사용자 id 설정: {}", cookie.getValue());
                    } catch (NumberFormatException e) {
                        logger.warn("쿠키에서 id 파싱 실패: {}", cookie.getValue());
                    }
                }
            }
        } else {
            logger.warn("쿠키가 없습니다. 기본 인구통계 정보를 사용합니다.");
        }

        // 캐시 키 로깅 - getCacheKey() 호출
        logger.info("생성된 캐시 키: {}", requestDto.getCacheKey());

        // 2) 추천 API 호출 및 raw JSON String으로 받기
        String rawJson = recommendationWebClient.post()
                .body(Mono.just(requestDto), UserRecommendationRequestDto.class)
                .retrieve()
                .bodyToMono(String.class) // String으로 받음
                .block();

        logger.info("=== [FastAPI Raw JSON] ===\n{}", rawJson);

        // 3) 디버깅용: rawJson을 Map으로 파싱
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = mapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
            logger.info("=== [Parsed as Map] ===\n{}", responseMap);
        } catch (Exception e) {
            logger.warn("rawJson -> Map 변환 실패", e);
        }

        // 4) rawJson을 RecommendationResponseDto로 파싱
        RecommendationResponseDto originalResponse;
        try {
            ObjectMapper mapper = new ObjectMapper();
            originalResponse = mapper.readValue(rawJson, RecommendationResponseDto.class);
        } catch (Exception e) {
            logger.error("rawJson -> RecommendationResponseDto 변환 실패", e);
            throw new RecommendationException("Parsing error: " + e.getMessage());
        }

        // 5) 추천 결과가 없으면 예외 처리
        if (originalResponse == null
                || originalResponse.getRecommendedProperties() == null
                || originalResponse.getRecommendedProperties().isEmpty()) {
            throw new RecommendationException("No recommendations received from Python API.");
        }

        List<RecommendationDto> recList = originalResponse.getRecommendedProperties();

        // ★ 주의: 여기서는 찜 여부(liked)는 기본값(false)으로 설정 (후처리로 최신화)
        List<DetailedRecommendationDto> detailedList = new ArrayList<>();

        for (RecommendationDto rec : recList) {
            try {
                Property property = propertyService.getPropertyDetail((long) rec.getPropertyId());
                PropertyScore score = propertyService.getPropertyScoreByPropertyId(Math.toIntExact(rec.getPropertyId()));

                DetailedRecommendationWithLikedDto dto = new DetailedRecommendationWithLikedDto();
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
                dto.setLatitude(property.getLatitude());
                dto.setLongitude(property.getLongitude());
                dto.setSimilarity(rec.getSimilarity());

                // 찜 여부는 기본값(false)로 초기화 (후처리 단계에서 별도로 업데이트)
                dto.setLiked(false);

                detailedList.add(dto);
            } catch (PropertyNotFoundException ex) {
                logger.warn("Property not found for id: {}", rec.getPropertyId());
            }
        }

        DetailedRecommendationResponseDto detailedResponse = new DetailedRecommendationResponseDto();
        detailedResponse.setRecommendedProperties(detailedList);
        detailedResponse.setMaxType(originalResponse.getMaxType());
        return detailedResponse;
    }
}
