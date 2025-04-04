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
     * 모든 점수 중 가장 높은 점수를 반환합니다.
     * 만약 모든 점수가 null이면 0.0을 반환합니다.
     *
     * @return 가장 높은 점수
     */
    public Double getMaxScore() {
        return Stream.of(transportScore, restaurantScore, healthScore, convenienceScore, cafeScore, chickenScore, leisureScore)
                .filter(Objects::nonNull)
                .max(Double::compare)
                .orElse(0.0);
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
