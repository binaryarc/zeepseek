package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.dong.entity.DongInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zeepseek.backend.domain.dong.repository.MySQLDongRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private final WebClient recommendationWebClient;
    private final PropertyService propertyService;
    private final MySQLDongRepository dongRepository;

    public RecommendationServiceImpl(WebClient recommendationWebClient,
                                     PropertyService propertyService,
                                     MySQLDongRepository dongRepository) {
        this.recommendationWebClient = recommendationWebClient;
        this.propertyService = propertyService;
        this.dongRepository = dongRepository;
    }

    @Override
    public DetailedRecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto, HttpServletRequest request) {
        // 1) 쿠키에서 age와 gender 정보 추출
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
                }
            }
        } else {
            logger.warn("쿠키가 없습니다. 기본 인구통계 정보를 사용합니다.");
        }

        // 2) 먼저 raw JSON(String)으로 받아봄
        String rawJson = recommendationWebClient.post()
                .body(Mono.just(requestDto), UserRecommendationRequestDto.class)
                .retrieve()
                .bodyToMono(String.class) // String으로 받음
                .block();

        logger.info("=== [FastAPI Raw JSON] ===\n{}", rawJson);

        // 3) rawJson을 Map으로 한번 파싱 (디버깅용)
        //    어떤 구조와 키(key)가 넘어오는지 직접 확인 가능
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = mapper.readValue(rawJson, new TypeReference<Map<String, Object>>(){});
            logger.info("=== [Parsed as Map] ===\n{}", responseMap);
        } catch (Exception e) {
            logger.warn("rawJson -> Map 변환 실패", e);
        }

        // 4) rawJson을 다시 RecommendationResponseDto로 파싱
        RecommendationResponseDto originalResponse;
        try {
            ObjectMapper mapper = new ObjectMapper();
            originalResponse = mapper.readValue(rawJson, RecommendationResponseDto.class);
        } catch (Exception e) {
            logger.error("rawJson -> RecommendationResponseDto 변환 실패", e);
            throw new RecommendationException("Parsing error: " + e.getMessage());
        }

        // 5) 이제 originalResponse를 기존 로직처럼 처리
        if (originalResponse == null
                || originalResponse.getRecommendedProperties() == null
                || originalResponse.getRecommendedProperties().isEmpty()) {
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
                dto.setLatitude(property.getLatitude());
                dto.setLongitude(property.getLongitude());
                dto.setSimilarity(rec.getSimilarity());

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
        detailedResponse.setMaxType(originalResponse.getMaxType());
        return detailedResponse;
    }

    @Override
    public AiRecommendationResponseDto getAiRecommendation(Integer userId) {
        AiRecommendationFastApiResponseDto originalResponse = recommendationWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .replacePath("/ai-recommend")
                        .queryParam("user_id", userId)
                        .build())
                .retrieve()
                .bodyToMono(AiRecommendationFastApiResponseDto.class)
                .block();

        if (originalResponse == null || originalResponse.getPropertyIds() == null ||
                originalResponse.getPropertyIds().isEmpty()) {
            logger.info("추천 결과가 없습니다. 빈 배열을 반환합니다.");
            return new AiRecommendationResponseDto(); // 빈 배열과 dongId null 반환
        }

        List<Long> propertyIdList = originalResponse.getPropertyIds();
        List<Property> detailedList = new ArrayList<>();

        for (Long propId : propertyIdList) {
            try {
                Property property = propertyService.getPropertyDetail(propId);
                detailedList.add(property);
            } catch (PropertyNotFoundException ex) {
                logger.warn("Property not found for id: {}", propId);
            }
        }

        AiRecommendationResponseDto detailedResponse = new AiRecommendationResponseDto();
        detailedResponse.setRecommendedProperties(detailedList);

        String dongIdentifier = originalResponse.getDongId();
        if (dongIdentifier != null) {
            try {
                Integer dongId = Integer.parseInt(dongIdentifier);
                dongRepository.findById(dongId)
                        .ifPresent(dongInfo -> detailedResponse.setDongName(dongInfo.getName()));
            } catch (NumberFormatException e) {
                logger.warn("dongId 파싱 실패: {}", dongIdentifier);
            }
        }
        return detailedResponse;
    }
}
