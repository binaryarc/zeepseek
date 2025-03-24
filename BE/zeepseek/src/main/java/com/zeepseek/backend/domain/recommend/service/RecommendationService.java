package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.response.RecommendationDto;
import com.zeepseek.backend.domain.recommend.dto.request.UserCategoryScoreRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecommendationService {

    /**
     * 사용자 점수를 기반으로 추천 매물 목록을 페이지네이션하여 반환합니다.
     *
     * @param userCategoryScoreDto 사용자 점수를 담은 DTO
     * @param pageable             페이지네이션 정보
     * @return 추천 매물 목록 (Page)
     */
    Page<RecommendationDto> getRecommendations(UserCategoryScoreRequestDto userCategoryScoreDto, Pageable pageable);
}
