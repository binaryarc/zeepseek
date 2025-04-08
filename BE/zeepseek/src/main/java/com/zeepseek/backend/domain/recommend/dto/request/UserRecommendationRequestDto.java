package com.zeepseek.backend.domain.recommend.dto.request;

import lombok.Data;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
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

    // # 추가된 필터 조건: 가격범위, 방 유형, 계약 유형
    private List<Integer> priceRange; // 예: [500000, 1000000]
    private String roomType;          // 예: "원룸", "투룸", "빌라", "주택"
    private String contractType;      // 예: "단기임대", "월세", "전세", "매매"

    /**
     * 캐시 키 생성 메서드
     * (userId, 각 선호도 점수와 추가 필터 조건들을 연결하여 고유한 키 생성)
     */
    public String getCacheKey() {
        // 각 값이 null이면 빈 문자열("")로 대체하여 연결합니다.
        String priceRangeKey = (priceRange != null && !priceRange.isEmpty()) ? priceRange.toString() : "";
        String roomTypeKey = (roomType != null) ? roomType : "";
        String contractTypeKey = (contractType != null) ? contractType : "";

        return userId + "_" +
                transportScore + "_" +
                restaurantScore + "_" +
                healthScore + "_" +
                convenienceScore + "_" +
                cafeScore + "_" +
                chickenScore + "_" +
                leisureScore + "_" +
                priceRangeKey + "_" +
                roomTypeKey + "_" +
                contractTypeKey;
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
