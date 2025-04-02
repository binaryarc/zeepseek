package com.zeepseek.backend.domain.auth.service;

import com.zeepseek.backend.domain.auth.dto.TokenDto;
import com.zeepseek.backend.domain.user.dto.UserDto;
import com.zeepseek.backend.domain.user.dto.UserProfileDto;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    /**
     * 토큰 갱신 (액세스 토큰 만료 시)
     */
    TokenDto refreshToken(String refreshToken);

    /**
     * 로그아웃 처리
     */
    void logout(String accessToken);

    /**
     * 소셜 로그인 처리
     */
    TokenDto processSocialLogin(String authorizationCode, String provider);

    /**
     * 토큰으로 현재 사용자 정보 조회
     * (내부적으로 UserService.getUserById()를 호출)
     */
    UserDto getCurrentUserByToken(String accessToken);
}