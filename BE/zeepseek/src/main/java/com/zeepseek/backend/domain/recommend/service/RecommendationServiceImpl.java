package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.request.UserRecommendationRequestDto;
import com.zeepseek.backend.domain.recommend.dto.RecommendationDto;
import com.zeepseek.backend.domain.recommend.dto.response.RecommendationResponseDto;
import com.zeepseek.backend.domain.recommend.exception.RecommendationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final WebClient webClient;

    // WebClient 생성자 주입
    public RecommendationServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public RecommendationResponseDto getRecommendations(UserRecommendationRequestDto requestDto, Pageable pageable) {
        try {
            // 요청 로깅
            logger.info("Sending recommendation request to: {}", webClient.toString());
            logger.debug("Request DTO: {}", requestDto);

            // WebClient를 통한 POST 요청
            RecommendationResponseDto response = webClient.post()
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(RecommendationResponseDto.class)
                    .doOnError(e -> logger.error("WebClient error: ", e))
                    .block();

            // 응답 검증
            if (response == null || response.getRecommendedProperties() == null) {
                throw new RecommendationException("No recommendations received from Python API.");
            }

            // 페이징 처리
            List<RecommendationDto> allRecommendations = response.getRecommendedProperties();
            int totalElements = allRecommendations.size();
            int pageSize = pageable.getPageSize();
            int currentPage = pageable.getPageNumber();
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, totalElements);
            List<RecommendationDto> pagedRecommendations = allRecommendations.subList(start, end);

            // 응답 객체 생성
            RecommendationResponseDto result = new RecommendationResponseDto();
            result.setRecommendedProperties(pagedRecommendations);
            result.setTotalElements(totalElements);
            result.setTotalPages((int) Math.ceil((double) totalElements / pageSize));
            result.setCurrentPage(currentPage);

            // 로깅
            logger.info("Returning page {} with {} recommendations out of {} total.",
                    currentPage, pagedRecommendations.size(), totalElements);

            return result;

        } catch (Exception e) {
            logger.error("Error fetching recommendations: {}", e.getMessage(), e);
            throw new RecommendationException("Failed to fetch recommendations.", e);
        }
    }
}