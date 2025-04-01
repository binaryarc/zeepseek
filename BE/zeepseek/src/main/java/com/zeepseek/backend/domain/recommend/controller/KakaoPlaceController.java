package com.zeepseek.backend.domain.recommend.controller;


import com.zeepseek.backend.domain.recommend.service.KakaoPlaceService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
public class KakaoPlaceController {

    private final KakaoPlaceService kakaoPlaceService;

    public KakaoPlaceController(KakaoPlaceService kakaoPlaceService) {
        this.kakaoPlaceService = kakaoPlaceService;
    }

    /**
     * 예시 요청 URL:
     * GET /api/places/search?category=FD6&x=127.1054328&y=37.3595963
     */
    @GetMapping("/search")
    public Mono<List<?>> searchPlaces(
            @RequestParam("type") String type,
            @RequestParam("x") String longitude,
            @RequestParam("y") String latitude) {
        return kakaoPlaceService.findPlacesWithinOneKmByType(type, longitude, latitude);
    }
}
