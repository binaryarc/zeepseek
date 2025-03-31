package com.zeepseek.backend.domain.user.controller;

import com.zeepseek.backend.domain.auth.dto.ApiResponse;
import com.zeepseek.backend.domain.auth.security.UserPrincipal;
import com.zeepseek.backend.domain.auth.util.CookieUtils;
import com.zeepseek.backend.domain.user.dto.UserDto;
import com.zeepseek.backend.domain.user.dto.UserProfileDto;
import com.zeepseek.backend.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth") // auth 경로 사용
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 첫 로그인 시 추가 데이터 처리
     */
    @PostMapping("/survey")
    public ResponseEntity<ApiResponse<UserDto>> firstLoginData(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UserProfileDto profileDto,
            HttpServletResponse response) {

        Integer userId = userPrincipal.getId();
        log.info("첫 로그인 데이터 처리: userId={}, profileDto={}", userId, profileDto);

        UserDto updatedUser = userService.processFirstLoginData(userId, profileDto);

        // 업데이트된 사용자 정보를 쿠키에 저장
        CookieUtils.addUserCookie(response, updatedUser);

        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    /**
     * 사용자 정보 조회
     */
    @GetMapping("/{idx}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("idx") Integer idx,
            HttpServletResponse response) {
        // 자신의 정보만 조회 가능하도록 검증
        if (!userPrincipal.getId().equals(idx)) {
            return ResponseEntity.status(403).body(ApiResponse.error("권한이 없습니다."));
        }

        UserDto userDto = userService.getUserById(idx);

        // 사용자 정보를 쿠키에 저장
        CookieUtils.addUserCookie(response, userDto);

        return ResponseEntity.ok(ApiResponse.success("정보 조회 성공", userDto));
    }

    /**
     * 회원 정보 수정
     */
    @PatchMapping("/{idx}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("idx") Integer idx,
            @RequestBody UserDto userDto,
            HttpServletResponse response) {
        // 자신의 정보만 수정 가능하도록 검증
        if (!userPrincipal.getId().equals(idx)) {
            return ResponseEntity.status(403).body(ApiResponse.error("권한이 없습니다."));
        }

        UserDto updatedUser = userService.updateUser(userPrincipal.getId(), userDto);

        // 업데이트된 사용자 정보를 쿠키에 저장
        CookieUtils.addUserCookie(response, updatedUser);

        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/{idx}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("idx") Integer idx,
            HttpServletRequest request,
            HttpServletResponse response) {
        // 자신의 계정만 삭제 가능하도록 검증
        if (!userPrincipal.getId().equals(idx)) {
            return ResponseEntity.status(403).body(ApiResponse.error("권한이 없습니다."));
        }

        userService.deleteUser(userPrincipal.getId());

        // 쿠키 삭제
        CookieUtils.deleteCookie(request, response, "user_info");
        CookieUtils.deleteCookie(request, response, "user_idx");

        return ResponseEntity.ok(ApiResponse.success("계정이 삭제되었습니다.", null));
    }

    /**
     * 프로필 완료 여부 확인
     */
    @GetMapping("/profile/complete")
    public ResponseEntity<ApiResponse<Boolean>> isProfileComplete(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        boolean isComplete = userService.isProfileComplete(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(isComplete));
    }
}