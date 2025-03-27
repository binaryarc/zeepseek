package com.zeepseek.backend.domain.auth.security.oauth2;

import com.zeepseek.backend.domain.auth.dto.TokenDto;
import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.repository.UserRepository;
import com.zeepseek.backend.domain.auth.security.UserPrincipal;
import com.zeepseek.backend.domain.auth.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("응답이 이미 커밋되었습니다. " + targetUrl + "로 리다이렉트할 수 없습니다");
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // JWT 토큰 생성
        TokenDto tokenDto = tokenProvider.generateToken(authentication);

        // 리프레시 토큰 DB에 저장
        Optional<User> userOptional = userRepository.findById(userPrincipal.getId());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setRefreshToken(tokenDto.getRefreshToken());
            userRepository.save(user);
        }

        int isFirst = userPrincipal.isFirst() ? 1 : 0;

        // 리다이렉트 URL 생성 (프론트엔드로 리다이렉트)
        return UriComponentsBuilder.fromUriString("https://j12e203.p.ssafy.io/auth/naver/callback")
                .queryParam("token", tokenDto.getAccessToken())
                .queryParam("refreshToken", tokenDto.getRefreshToken())
                .queryParam("isFirst", isFirst)
                .build().toUriString();
    }
}