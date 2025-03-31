package com.zeepseek.backend.domain.auth.controller;

import com.zeepseek.backend.domain.auth.dto.*;
import com.zeepseek.backend.domain.auth.service.AuthService;
import com.zeepseek.backend.domain.auth.util.CookieUtils;
import com.zeepseek.backend.domain.user.dto.UserDto;
import com.zeepseek.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService; // UserService 주입

    /**
     * 소셜 로그인 처리
     */
    @PostMapping("/api/v1/auth/sessions")
    public ResponseEntity<ApiResponse<TokenDto>> socialLogin(
            @RequestBody SocialLoginRequest request,
            HttpServletResponse response) {
        log.info("=== 소셜 로그인 요청 시작 ===");
        log.info("인증 코드: {}", request.getAuthorizationCode());
        log.info("제공자: {}", request.getProvider());

        try {
            // 인증 코드로 토큰 요청 및 사용자 정보 조회
            TokenDto tokenDto = authService.processSocialLogin(request.getAuthorizationCode(), request.getProvider());

            // 사용자 정보 조회 및 쿠키에 저장
            if (tokenDto.getUser() != null && tokenDto.getUser().getIdx() != null) {
                UserDto userDto = userService.getUserById(tokenDto.getUser().getIdx());
                CookieUtils.addUserCookie(response, userDto);
            }

            log.info("로그인 성공! 토큰 생성됨: accessToken={}, refreshToken={}",
                    tokenDto.getAccessToken().substring(0, 10) + "...",
                    tokenDto.getRefreshToken().substring(0, 10) + "...");

            return ResponseEntity.ok(ApiResponse.success(tokenDto));
        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 오류 발생", e);
            throw e;
        } finally {
            log.info("=== 소셜 로그인 요청 종료 ===");
        }
    }

    /**
     * 로그아웃 - access token 무효화
     */
    @DeleteMapping("/api/v1/auth/sessions")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Bearer 접두사 제거
        String accessToken = authHeader.replace("Bearer ", "");
        authService.logout(accessToken);

        // 쿠키 삭제
        CookieUtils.deleteCookie(request, response, "user_info");
        CookieUtils.deleteCookie(request, response, "user_idx");

        return ResponseEntity.ok(ApiResponse.success("성공적으로 로그아웃 되었습니다.", null));
    }

    /**
     * 토큰 갱신 - refresh token을 사용하여 access token 재발급
     */
    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<ApiResponse<TokenDto>> refreshToken(
            @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletResponse response) {
        TokenDto tokenDto = authService.refreshToken(refreshTokenRequest.getRefreshToken());

        // 사용자 정보 조회 및 쿠키에 저장
        if (tokenDto.getUser() != null && tokenDto.getUser().getIdx() != null) {
            UserDto userDto = userService.getUserById(tokenDto.getUser().getIdx());
            CookieUtils.addUserCookie(response, userDto);
        }

        return ResponseEntity.ok(ApiResponse.success(tokenDto));
    }

    /**
     * 로그인 후 리다이렉트 엔드포인트
     * - 인가 코드를 받아 처리하는 역할
     */
    @GetMapping("/api/v1/auth/redirect")
    public ResponseEntity<ApiResponse<TokenDto>> oauthRedirect(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("=== 소셜 로그인 리다이렉트 시작 ===");
        log.info("요청 URL: {}", request.getRequestURL());
        log.info("전체 URI: {}", request.getRequestURI());
        log.info("쿼리 스트링: {}", request.getQueryString());
        log.info("요청 메서드: {}", request.getMethod());
        log.info("인증 코드: {}", code);

        // 네이버 로그인인 경우 state 파라미터가 존재함
        String provider = state != null ? "naver" : "kakao";
        log.info("인증 제공자: {}", provider);

        try {
            // 인가 코드를 사용하여 로그인 처리
            log.info("authService.processSocialLogin 호출 중...");
            TokenDto tokenDto = authService.processSocialLogin(code, provider);

            // 사용자 정보 조회 및 쿠키에 저장
            if (tokenDto.getUser() != null && tokenDto.getUser().getIdx() != null) {
                UserDto userDto = userService.getUserById(tokenDto.getUser().getIdx());
                CookieUtils.addUserCookie(response, userDto);
            }

            log.info("로그인 성공! 토큰 생성됨: accessToken={}, refreshToken={}",
                    tokenDto.getAccessToken().substring(0, 10) + "...",
                    tokenDto.getRefreshToken().substring(0, 10) + "...");

            return ResponseEntity.ok(ApiResponse.success(tokenDto));
        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 오류 발생", e);
            throw e;
        } finally {
            log.info("=== 소셜 로그인 리다이렉트 종료 ===");
        }
    }
}