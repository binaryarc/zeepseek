package com.zeepseek.backend.domain.auth.service;

import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.repository.UserRepository;
import com.zeepseek.backend.domain.auth.dto.request.TokenRefreshRequest;
import com.zeepseek.backend.domain.auth.dto.response.AuthResponse;
import com.zeepseek.backend.domain.auth.exception.CustomException;
import com.zeepseek.backend.domain.auth.exception.ErrorCode;
import com.zeepseek.backend.domain.auth.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    /**
     * OAuth 로그인 처리
     */
    /**
     * OAuth 로그인 처리
     */
    @Transactional
    public AuthResponse oauthLogin(String authorizationCode, String provider) {
        log.info("OAuth 로그인 처리: provider={}, authorizationCode={}", provider, authorizationCode);

        // 유효한 provider 확인
        if (!("kakao".equals(provider) || "naver".equals(provider))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 소셜 로그인입니다.");
        }

        // 인증 코드를 providerId로 사용
        String providerId = authorizationCode;

        // 사용자 조회 또는 생성
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // 새 사용자 생성
                    User newUser = User.builder()
                            .provider(provider)
                            .providerId(providerId)
                            .nickname(provider + "_user_" + providerId.substring(0, Math.min(8, providerId.length())))
                            .isFirst(1) // 첫 로그인
                            .build();
                    return userRepository.save(newUser);
                });

        // 인증 객체 생성
        Authentication authentication = createAuthentication(user);

        // 토큰 발급
        String accessToken = tokenProvider.createAccessToken(authentication, user.getIdx());
        String refreshToken = tokenProvider.createRefreshToken();

        // 리프레시 토큰 저장
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        // 첫 로그인이 아닌 경우 isFirst 값 업데이트
        boolean isFirstLogin = user.getIsFirst() == 1;
        if (isFirstLogin) {
            user.markAsNotFirstLogin();
            userRepository.save(user);
        }

        // 응답 생성
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpiryDuration())
                .isFirstLogin(isFirstLogin)
                .build();
    }

    /**
     * 리프레시 토큰을 사용하여 새 액세스 토큰 발급
     */
    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        // 리프레시 토큰 유효성 검증
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 리프레시 토큰으로 사용자 조회
        User user = userRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 인증 객체 생성
        Authentication authentication = createAuthentication(user);

        // 새 액세스 토큰 발급
        String newAccessToken = tokenProvider.createAccessToken(authentication, user.getIdx());

        // 응답 생성
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpiryDuration())
                .isFirstLogin(user.getIsFirst() == 1)
                .build();
    }

    /**
     * 로그아웃 처리
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 리프레시 토큰 제거
        user.updateRefreshToken(null);
        userRepository.save(user);

        // 인증 정보 제거
        SecurityContextHolder.clearContext();
    }

    /**
     * 사용자 정보로 인증 객체 생성
     */
    private Authentication createAuthentication(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (user.getIsSeller() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
        }

        // 사용자 식별자를 Principal 객체로 사용
        String principal = user.getProvider() + "_" + user.getProviderId();

        // 인증 객체 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        return authenticationToken;
    }
}