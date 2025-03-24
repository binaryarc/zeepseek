package com.zeepseek.backend.domain.auth.controller;

import com.zeepseek.backend.domain.auth.dto.*;
import com.zeepseek.backend.domain.auth.security.UserPrincipal;
import com.zeepseek.backend.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 소셜 로그인 처리
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenDto>> socialLogin(@RequestBody SocialLoginRequest request) {
        // 인증 코드로 토큰 요청 및 사용자 정보 조회
        TokenDto tokenDto = authService.processSocialLogin(request.getAuthorizationCode(), request.getProvider());
        return ResponseEntity.ok(ApiResponse.success(tokenDto));
    }

    /**
     * 로그아웃 - access token 무효화
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        // Bearer 접두사 제거
        String accessToken = authHeader.replace("Bearer ", "");
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.success("성공적으로 로그아웃 되었습니다.", null));
    }

    /**
     * 토큰 갱신 - refresh token을 사용하여 access token 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenDto>> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        TokenDto tokenDto = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokenDto));
    }

    /**
     * 회원 정보 수정
     */
    @PatchMapping("/update")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UserDto userDto) {
        UserDto updatedUser = authService.updateUser(userPrincipal.getId(), userDto);
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        authService.deleteUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("계정이 삭제되었습니다.", null));
    }

    /**
     * 첫 로그인 시 추가 데이터 처리
     */
    @PostMapping("/first-data")
    public ResponseEntity<ApiResponse<UserDto>> firstLoginData(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UserDto userDto) {
        UserDto updatedUser = authService.processFirstLoginData(userPrincipal.getId(), userDto);
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    /**
     * 현재 사용자 정보 조회 (필요시)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserDto userDto = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    /**
     * 카카오 로그인 후 리다이렉트 엔드포인트
     * - 카카오에서 인가 코드를 받아 처리하는 역할
     */
    @GetMapping("/redirect")
    public ResponseEntity<ApiResponse<TokenDto>> kakaoRedirect(@RequestParam("code") String code) {
        log.info("카카오 로그인 요청 code: {}", code);

        // 인가 코드를 사용하여 로그인 처리 (카카오 토큰 요청 -> 사용자 정보 조회)
        TokenDto tokenDto = authService.processSocialLogin(code, "kakao");

        return ResponseEntity.ok(ApiResponse.success(tokenDto));
    }

}