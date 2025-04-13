package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationFastApiResponseDto {
    // FastAPI에서 넘어온 매물 id 목록
    private List<Long> propertyIds;
    
    // FastAPI에서 넘어온 dong id (문자열로 저장, 필요에 따라 변환 가능)
    private String dongId;
}
