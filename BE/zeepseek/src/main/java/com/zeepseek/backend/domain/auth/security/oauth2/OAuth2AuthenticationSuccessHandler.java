package com.zeepseek.backend.domain.auth.security.oauth2;

import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.repository.UserRepository;
import com.zeepseek.backend.domain.auth.exception.CustomException;
import com.zeepseek.backend.domain.auth.exception.ErrorCode;
import com.zeepseek.backend.domain.auth.security.jwt.JwtTokenProvider;
import com.zeepseek.backend.domain.auth.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final CookieUtils cookieUtils;

    // 프론트엔드 리다이렉트 URL (환경변수에서 가져오거나 하드코딩)
    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/success}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("응답이 이미 전송되었습니다. 리다이렉트할 수 없습니다: {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String redirectUri = frontendRedirectUri;

        if (!isValidRedirectUri(redirectUri)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 리다이렉트 URI입니다");
        }

        // JWT 토큰 생성
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();
        boolean isFirstLogin = userPrincipal.isFirstLogin();

        String accessToken = tokenProvider.createAccessToken(authentication, userId);
        String refreshToken = tokenProvider.createRefreshToken();

        // 리프레시 토큰 저장
        saveRefreshToken(userId, refreshToken);

        // 쿠키에 토큰 저장 (선택적)
        int cookieMaxAge = (int) (tokenProvider.getExpiryDuration() / 1000);
        cookieUtils.addCookie(response, "accessToken", accessToken, cookieMaxAge);

        // 프론트엔드로 리다이렉트 URL 생성 (토큰과 첫 로그인 여부 포함)
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("isFirst", isFirstLogin)
                .build().toUriString();
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        // 사용자 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 리프레시 토큰 업데이트
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);
    }

    // 리다이렉트 URI 유효성 검사
    private boolean isValidRedirectUri(String uri) {
        try {
            URI redirectUri = new URI(uri);
            // 여기서 허용된 도메인인지 추가 검사 가능
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 인증 관련 쿠키 제거
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        // 인증 관련 쿠키 제거 (필요시)
    }
}