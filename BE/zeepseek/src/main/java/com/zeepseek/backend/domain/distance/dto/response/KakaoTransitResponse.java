package com.zeepseek.backend.domain.distance.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class KakaoTransitResponse {
    private Integer walkingDuration;    // 도보 시간 (초)
    private Integer transitDuration;    // 대중교통 시간 (초)
    
    // 사용자 친화적인 시간 문자열 반환 메서드
    public String getWalkingTimeString() {
        if (walkingDuration == null) return "정보 없음";
        int minutes = walkingDuration / 60;
        if (minutes < 60) {
            return minutes + "분";
        } else {
            return (minutes / 60) + "시간 " + (minutes % 60) + "분";
        }
    }
    
    public String getTransitTimeString() {
        if (transitDuration == null) return "정보 없음";
        int minutes = transitDuration / 60;
        if (minutes < 60) {
            return minutes + "분";
        } else {
            return (minutes / 60) + "시간 " + (minutes % 60) + "분";
        }
    }
}