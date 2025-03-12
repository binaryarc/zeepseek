package com.ssafy.oauth.security.oauth2;

import com.ssafy.oauth.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final CookieUtils cookieUtils;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/success}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 인증 실패: {}", exception.getMessage());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", exception.getLocalizedMessage())
                .build().toUriString();

        // 인증 관련 쿠키 제거
        cookieUtils.deleteCookie(request, response, "accessToken");

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}