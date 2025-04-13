package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                   // @Getter + @Setter + @RequiredArgsConstructor + @ToString + @EqualsAndHashCode
@NoArgsConstructor      // 파라미터 없는 생성자
@AllArgsConstructor     // 모든 필드를 파라미터로 받는 생성자
@Builder                // 빌더 패턴 생성
public class DetailedAiRecommendationDto {

    private Integer propertyId;
    private String address;
    private String roomType;
    private String contractType;
    private Integer deposit;
    private Integer monthlyRent;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}