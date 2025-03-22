package com.zeepseek.backend.domain.auth.controller;

import com.zeepseek.backend.domain.auth.dto.request.OAuthLoginRequest;
import com.zeepseek.backend.domain.auth.dto.request.TokenRefreshRequest;
import com.zeepseek.backend.domain.auth.dto.response.ApiResponse;
import com.zeepseek.backend.domain.auth.dto.response.AuthResponse;
import com.zeepseek.backend.domain.auth.service.AuthService;
import com.zeepseek.backend.domain.auth.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    /**
     * 액세스 토큰 갱신 엔드포인트
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        log.info("토큰 갱신 요청");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "토큰이 성공적으로 갱신되었습니다."));
    }

    /**
     * 로그아웃 엔드포인트
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("로그아웃 요청");
        Long userId = securityUtils.getCurrentUserId();
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody OAuthLoginRequest request) {
        log.info("OAuth 로그인 요청: provider={}", request.getProvider());
        AuthResponse response = authService.oauthLogin(request.getProviderId(), request.getProvider());
        return ResponseEntity.ok(ApiResponse.success(response, "로그인에 성공했습니다."));
    }

    /**
     * 인증 상태 확인 엔드포인트
     * GET /api/v1/auth/check
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkAuth() {
        try {
            securityUtils.getCurrentUserId(); // 인증이 유효한지 확인
            return ResponseEntity.ok(ApiResponse.success(true, "유효한 인증입니다."));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(false, "인증이 유효하지 않습니다."));
        }
    }
}