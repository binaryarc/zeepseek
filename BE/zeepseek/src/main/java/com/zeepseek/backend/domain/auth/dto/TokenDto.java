package com.zeepseek.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpireTime;
    private Long refreshTokenExpireTime;
    private Integer isFirst; // 첫 로그인 여부 (API 명세에 따라 필요)
    private UserDto user;
}