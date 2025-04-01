package com.zeepseek.backend.domain.auth.security.oauth2;

import com.zeepseek.backend.domain.auth.dto.TokenDto;
import com.zeepseek.backend.domain.auth.security.UserPrincipal;
import com.zeepseek.backend.domain.auth.security.jwt.JwtTokenProvider;
import com.zeepseek.backend.domain.auth.util.CookieUtils;
import com.zeepseek.backend.domain.user.dto.UserDto;
import com.zeepseek.backend.domain.user.entity.User;
import com.zeepseek.backend.domain.user.repository.UserRepository;
import com.zeepseek.backend.domain.user.service.UserService;
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
    private final UserService userService;
    private final String REDIRECT_BASE_URL = "https://j12e203.p.ssafy.io";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // JWT 토큰 생성
        TokenDto tokenDto = tokenProvider.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // 사용자 정보 조회
        UserDto userDto = userService.getUserById(userPrincipal.getId());

        // 세 가지 쿠키 모두 설정
        CookieUtils.addAllUserCookies(response, userDto, tokenDto.getRefreshToken());

        // 리다이렉트 URL 생성
        String targetUrl = determineTargetUrl(request, response, authentication, tokenDto);

        if (response.isCommitted()) {
            log.debug("응답이 이미 커밋되었습니다. " + targetUrl + "로 리다이렉트할 수 없습니다");
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication, TokenDto tokenDto) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // 리프레시 토큰 DB에 저장
        Optional<User> userOptional = userRepository.findById(userPrincipal.getId());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setRefreshToken(tokenDto.getRefreshToken());
            userRepository.save(user);
        }

        int isFirst = userPrincipal.isFirst() ? 1 : 0;

        // 첫 로그인 경로 수정: 프론트엔드의 설문 입력 페이지로 리다이렉트
        String redirectPath = isFirst == 1 ? "/survey" : "/auth/" + (authentication.getName().startsWith("kakao_") ? "kakao" : "naver") + "/callback";

        // 리다이렉트 URL 생성
        return UriComponentsBuilder.fromUriString(REDIRECT_BASE_URL + redirectPath)
                .queryParam("token", tokenDto.getAccessToken())
                .queryParam("refreshToken", tokenDto.getRefreshToken())
                .queryParam("isFirst", isFirst)
                .queryParam("idx", userPrincipal.getId())
                .build().toUriString();
    }

    /**
     * 오버로딩 메서드 추가
     */
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        TokenDto tokenDto = tokenProvider.generateToken(authentication);
        return determineTargetUrl(request, response, authentication, tokenDto);
    }
}