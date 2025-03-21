package com.zeepseek.backend.domain.auth.service;

import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    /**
     * 사용자 ID로 사용자 정보 조회
     */
    UserResponse getUserById(Long userId);

    /**
     * 닉네임으로 사용자 정보 조회
     */
    UserResponse getUserByNickname(String nickname);

    /**
     * 닉네임 중복 체크
     */
    boolean isNicknameAvailable(String nickname);

    /**
     * 사용자 정보 업데이트 (닉네임, 성별, 나이)
     */
    UserResponse updateUserInfo(Long userId, String nickname, Integer gender, Integer age);

    /**
     * 판매자 상태 전환 (일반 → 판매자, 판매자 → 일반)
     */
    UserResponse toggleSellerStatus(Long userId);

    /**
     * 사용자의 첫 로그인 여부 확인
     */
    boolean isFirstLogin(Long userId);

    /**
     * 사용자의 첫 로그인 상태 업데이트
     */
    void updateFirstLoginStatus(Long userId);

    /**
     * 판매자 목록 조회
     */
    List<UserResponse> getSellers();

    /**
     * 현재 인증된 사용자 정보 조회
     */
    User getCurrentUser();
}