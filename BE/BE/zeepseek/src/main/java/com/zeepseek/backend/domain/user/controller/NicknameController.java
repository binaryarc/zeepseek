package com.zeepseek.backend.domain.user.controller;

import com.zeepseek.backend.domain.auth.dto.ApiResponse;
import com.zeepseek.backend.domain.user.service.NicknameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class NicknameController {

    private final NicknameService nicknameService;

    /**
     * 랜덤 닉네임 생성
     */
    @GetMapping("/random-nickname")
    public ResponseEntity<ApiResponse<String>> generateRandomNickname() {
        String randomNickname = nicknameService.generateRandomNickname();
        log.info("Random nickname generated: {}", randomNickname);
        return ResponseEntity.ok(ApiResponse.success(randomNickname));
    }
}