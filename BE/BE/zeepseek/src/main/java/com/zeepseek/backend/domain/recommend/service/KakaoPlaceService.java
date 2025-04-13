package com.zeepseek.backend.domain.recommend.service;

import reactor.core.publisher.Mono;

import java.util.List;

public interface KakaoPlaceService {

    /**
     * 지정한 위도, 경도 기준 반경 1km 내에 특정 타입의 장소 정보를 조회합니다.
     * 지원하는 타입:
     * <ul>
     *     <li>"cafe", "restaurant", "medical" → 각각 "CE7", "FD6", "HP8"</li>
     *     <li>"transport", "leisure", "convenience" → 각각 "SW8", "AT4", "CS2"</li>
     *     <li>"chicken" → DB 저장 프로시저를 호출하여 치킨집 정보를 조회 (음식점 카테고리 "FD6"로 처리)</li>
     * </ul>
     *
     * @param type      검색할 장소 타입
     * @param longitude 기준 위치의 경도
     * @param latitude  기준 위치의 위도
     * @return 해당 장소들의 이름, 위도, 경도를 담은 리스트를 Mono로 반환
     */
    Mono<List<?>> findPlacesWithinOneKmByType(String type, String longitude, String latitude);
}
