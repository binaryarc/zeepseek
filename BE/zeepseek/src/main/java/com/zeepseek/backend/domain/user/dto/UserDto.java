package com.zeepseek.backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Integer idx;
    private Integer isFirst;
    private Integer isSeller;
    private Integer gender;
    private Integer age;
    private String nickname;
    private String provider;
    private String refreshToken;
    private UserDto user;
}