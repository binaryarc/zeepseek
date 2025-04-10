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
import com.zeepseek.backend.domain.zzim.document.PropertyZzimDoc;
import com.zeepseek.backend.domain.zzim.service.ZzimService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private final WebClient recommendationWebClient;
    private final PropertyService propertyService;
    private final MySQLDongRepository dongRepository;
    private final ZzimService zzimService;

    public RecommendationServiceImpl(WebClient recommendationWebClient,
                                     PropertyService propertyService,
                                     MySQLDongRepository dongRepository,
                                     ZzimService zzimService) {
        this.recommendationWebClient = recommendationWebClient;
        this.propertyService = propertyService;
        this.dongRepository = dongRepository;
        this.zzimService = zzimService;
    }

    @Override
    @Cacheable(value = "recommendations", key = "#requestDto.getCacheKey()", unless = "#result == null")
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

        // ★ 주의: 여기서는 찜 여부(liked)는 기본값(false)으로 설정(이후 후처리로 최신화)
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

                // 찜 여부는 기본값(false)로 초기화(후처리 단계에서 별도로 업데이트)
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

    /**
     * 캐시된 추천 결과에 대해 찜(zzim) 여부를 최신 정보로 업데이트하는 후처리 메서드
     */
    private void updateLikedStatus(DetailedRecommendationResponseDto recommendations, Long userId) {
        // 최신 찜 목록을 조회 (여기서는 zzimService를 호출)
        List<PropertyZzimDoc> zzimDocs = zzimService.userSelectPropertyList(userId.intValue());
        Set<Integer> likedPropertyIds = zzimDocs.stream()
                .map(PropertyZzimDoc::getPropertyId)
                .collect(Collectors.toSet());

        if (recommendations != null && recommendations.getRecommendedProperties() != null) {
            for (DetailedRecommendationDto dto : recommendations.getRecommendedProperties()) {
                dto.setLiked(likedPropertyIds.contains(dto.getPropertyId()));
            }
        }
    }

    /**
     * 캐싱된 추천 결과를 받아온 뒤, 찜 여부를 후처리하여 반환하는 wrapper 메서드
     * (이 메서드를 컨트롤러에서 호출할 경우, 캐싱된 추천 결과에 대해 최신 찜 정보를 업데이트할 수 있습니다.)
     */
    public DetailedRecommendationResponseDto getRecommendationsWithUpdatedLikes(UserRecommendationRequestDto requestDto, HttpServletRequest request) {
        // 캐시된 추천 결과 조회
        DetailedRecommendationResponseDto cachedResponse = getRecommendations(requestDto, request);
        // 후처리: 찜 여부 업데이트
        updateLikedStatus(cachedResponse, requestDto.getUserId());
        return cachedResponse;
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
