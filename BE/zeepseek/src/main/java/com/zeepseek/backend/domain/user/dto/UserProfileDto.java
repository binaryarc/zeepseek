package com.zeepseek.backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    // 사용자 기본 정보
    private Integer gender; // 0: 선택안함, 1: 남자, 2: 여자
    private Integer age;
    private String location; // 기준 위치 (예: "멀티캠퍼스 역삼")
    
    // 매물 고려사항 (각 항목 선택 여부 - "안전", "편의", "식당" 등)
    private List<String> preferences;
    private String nickname;
}