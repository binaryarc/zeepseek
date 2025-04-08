package com.zeepseek.backend.domain.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingResponse {
    private int propertyId;
    private double score; // 조회수 또는 ranking count
}
