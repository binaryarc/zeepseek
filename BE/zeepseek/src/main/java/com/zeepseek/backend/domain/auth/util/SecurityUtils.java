package com.zeepseek.backend.domain.auth.util;

import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.repository.UserRepository;
import com.zeepseek.backend.domain.auth.exception.CustomException;
import com.zeepseek.backend.domain.auth.exception.ErrorCode;
import com.zeepseek.backend.domain.auth.security.jwt.JwtTokenProvider;
import com.zeepseek.backend.domain.auth.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    /**
     * 현재 인증된 사용자 ID 가져오기
     */
    public int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // UserPrincipal에서 ID 추출
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }

        // JWT 토큰인 경우
        if (authentication.getCredentials() instanceof String) {
            String token = (String) authentication.getCredentials();
            // 이 메소드도 int를 반환하도록 변경해야 합니다
            return tokenProvider.getUserIdFromToken(token);
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 현재 인증된 사용자 정보 가져오기
     */
    public User getCurrentUser() {
        Integer userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 현재 사용자가 판매자인지 확인
     */
    public boolean isCurrentUserSeller() {
        User currentUser = getCurrentUser();
        return currentUser.getIsSeller() == 1;
    }
}