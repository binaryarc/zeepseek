package com.zeepseek.backend.domain.auth.service;

import com.zeepseek.backend.domain.auth.dto.TokenDto;
import com.zeepseek.backend.domain.auth.dto.UserDto;

public interface AuthService {

    // 토큰 갱신 (액세스 토큰 만료 시)
    TokenDto refreshToken(String refreshToken);

    // 로그아웃 처리
    void logout(String accessToken);

    // 회원 정보 업데이트
    UserDto updateUser(Integer userId, UserDto userDto);

    // 회원 탈퇴
    void deleteUser(Integer userId);

    // 첫 로그인 시 추가 데이터 처리
    UserDto processFirstLoginData(Integer userId, UserDto userDto);

    // 현재 로그인된 사용자 정보 조회
    UserDto getCurrentUser(Integer userId);

    // 소셜 로그인 처리
    TokenDto processSocialLogin(String authorizationCode, String provider);
}