package com.ssafy.oauth.util;

import com.ssafy.oauth.domain.entity.User;
import com.ssafy.oauth.domain.repository.UserRepository;
import com.ssafy.oauth.exception.CustomException;
import com.ssafy.oauth.exception.ErrorCode;
import com.ssafy.oauth.security.jwt.JwtTokenProvider;
import com.ssafy.oauth.security.oauth2.UserPrincipal;
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
    public Long getCurrentUserId() {
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
            return tokenProvider.getUserIdFromToken(token);
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 현재 인증된 사용자 정보 가져오기
     */
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
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