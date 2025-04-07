package com.zeepseek.backend.domain.recommend.dto.request;

import lombok.Data;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

@Data
public class UserRecommendationRequestDto {
    private Long userId;
    private Double transportScore;
    private Double restaurantScore;
    private Double healthScore;
    private Double convenienceScore;
    private Double cafeScore;
    private Double chickenScore;
    private Double leisureScore;
    private Integer age;
    private Integer gender;

    /**
     * 캐시 키 생성 메서드
     * (userId와 각 선호도 점수를 연결하여 고유한 키 생성)
     */
    public String getCacheKey() {
        return userId + "_" +
                transportScore + "_" +
                restaurantScore + "_" +
                healthScore + "_" +
                convenienceScore + "_" +
                cafeScore + "_" +
                chickenScore + "_" +
                leisureScore;
    }

    /**
     * 가장 높은 점수를 가진 타입의 이름을 반환합니다.
     * 각 점수 변수명에서 "Score" 부분을 제외한 이름을 반환합니다.
     * 예를 들어 transportScore가 가장 높으면 "transport"를 반환합니다.
     *
     * @return 가장 높은 점수를 가진 타입 이름 (모두 null이면 빈 문자열)
     */
    public String getMaxScoreType() {
        return Stream.of(
                        new AbstractMap.SimpleEntry<>("transport", transportScore),
                        new AbstractMap.SimpleEntry<>("restaurant", restaurantScore),
                        new AbstractMap.SimpleEntry<>("health", healthScore),
                        new AbstractMap.SimpleEntry<>("convenience", convenienceScore),
                        new AbstractMap.SimpleEntry<>("cafe", cafeScore),
                        new AbstractMap.SimpleEntry<>("chicken", chickenScore),
                        new AbstractMap.SimpleEntry<>("leisure", leisureScore)
                )
                .filter(entry -> entry.getValue() != null)
                .max(Comparator.comparing(AbstractMap.SimpleEntry::getValue))
                .map(AbstractMap.SimpleEntry::getKey)
                .orElse("");
    }
}
