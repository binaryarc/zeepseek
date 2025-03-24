package com.zeepseek.backend.domain.auth.controller;

import com.zeepseek.backend.domain.auth.dto.response.ApiResponse;
import com.zeepseek.backend.domain.auth.dto.response.UserResponse;
import com.zeepseek.backend.domain.auth.service.UserService;
import com.zeepseek.backend.domain.auth.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    /**
     * 현재 로그인한 사용자 정보 조회
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        log.info("현재 사용자 정보 조회");
        int userId = securityUtils.getCurrentUserId();
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    /**
     * 사용자 ID로 사용자 정보 조회
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable int userId) {
        log.info("사용자 ID로 정보 조회: {}", userId);
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    // 다른 메소드들은 그대로 유지...

    /**
     * 사용자 정보 업데이트
     * PUT /api/v1/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserInfo(@RequestBody Map<String, Object> updateData) {
        log.info("사용자 정보 업데이트 요청");
        int userId = securityUtils.getCurrentUserId();

        String nickname = updateData.get("nickname") != null ?
                (String) updateData.get("nickname") : null;

        Integer gender = updateData.get("gender") != null ?
                ((Number) updateData.get("gender")).intValue() : null;

        Integer age = updateData.get("age") != null ?
                ((Number) updateData.get("age")).intValue() : null;

        UserResponse updatedUser = userService.updateUserInfo(userId, nickname, gender, age);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "사용자 정보가 업데이트되었습니다."));
    }

    /**
     * 판매자 상태 전환
     * PUT /api/v1/users/me/seller
     */
    @PutMapping("/me/seller")
    public ResponseEntity<ApiResponse<UserResponse>> toggleSellerStatus() {
        log.info("판매자 상태 전환 요청");
        int userId = securityUtils.getCurrentUserId();
        UserResponse updatedUser = userService.toggleSellerStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "판매자 상태가 변경되었습니다."));
    }

    // 판매자 목록 조회 메소드는 그대로 유지...

    /**
     * 첫 로그인 상태 업데이트
     * PUT /api/v1/users/me/first-login
     */
    @PutMapping("/me/first-login")
    public ResponseEntity<ApiResponse<Void>> updateFirstLoginStatus() {
        log.info("첫 로그인 상태 업데이트 요청");
        int userId = securityUtils.getCurrentUserId();
        userService.updateFirstLoginStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "첫 로그인 상태가 업데이트되었습니다."));
    }

    /**
     * 첫 로그인 여부 확인
     * GET /api/v1/users/me/first-login
     */
    @GetMapping("/me/first-login")
    public ResponseEntity<ApiResponse<Boolean>> isFirstLogin() {
        log.info("첫 로그인 여부 확인 요청");
        int userId = securityUtils.getCurrentUserId();
        boolean isFirst = userService.isFirstLogin(userId);
        return ResponseEntity.ok(ApiResponse.success(isFirst));
    }
}