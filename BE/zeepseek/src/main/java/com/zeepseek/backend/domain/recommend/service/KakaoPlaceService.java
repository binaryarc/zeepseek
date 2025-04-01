package com.zeepseek.backend.domain.recommend.service;

import com.zeepseek.backend.domain.recommend.dto.response.PlaceInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface KakaoPlaceService {

    /**
     * 지정한 위도, 경도 기준 반경 1km 내에 특정 타입의 장소 정보를 조회합니다.
     * 지원하는 타입:
     * - "cafe", "restaurant", "medical" → 각각 "CE7", "FD6", "HP8"
     * - "transport", "leisure", "convenience", "chicken" → 각각 "SW8", "AT4", "CS2", "FD6" (치킨은 음식점 카테고리로 처리)
     *
     * @param type 검색할 장소 타입
     * @param longitude 기준 위치의 경도
     * @param latitude 기준 위치의 위도
     * @return 해당 장소들의 이름, 위도, 경도를 담은 리스트를 Mono로 반환
     */
    Mono<List<PlaceInfo>> findPlacesWithinOneKmByType(String type, String longitude, String latitude);
}
