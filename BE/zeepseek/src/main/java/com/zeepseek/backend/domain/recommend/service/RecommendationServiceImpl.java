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
    private final RecommendationCacheService recommendationCacheService;

    public RecommendationServiceImpl(WebClient recommendationWebClient,
                                     PropertyService propertyService,
                                     MySQLDongRepository dongRepository,
                                     ZzimService zzimService, RecommendationCacheService recommendationCacheService) {
        this.recommendationWebClient = recommendationWebClient;
        this.propertyService = propertyService;
        this.dongRepository = dongRepository;
        this.zzimService = zzimService;
        this.recommendationCacheService = recommendationCacheService;
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
     * 캐시된 추천 결과를 받아온 뒤, 찜 여부를 후처리하여 반환하는 wrapper 메서드
     * (이 메서드를 컨트롤러에서 호출할 경우, 캐싱된 추천 결과에 대해 최신 찜 정보를 업데이트할 수 있습니다.)
     */
    @Override
    public DetailedRecommendationResponseDto getRecommendationsWithUpdatedLikes(UserRecommendationRequestDto requestDto, HttpServletRequest request) {
        // 캐시된 추천 결과 조회
        DetailedRecommendationResponseDto cachedResponse = recommendationCacheService.getRecommendations(requestDto, request);
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
