package com.zeepseek.backend.domain.user.service;

import com.zeepseek.backend.domain.user.dto.UserDto;
import com.zeepseek.backend.domain.user.dto.UserProfileDto;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    /**
     * 회원 정보 업데이트
     */
    UserDto updateUser(Integer userId, UserDto userDto);

    /**
     * 회원 탈퇴
     */
    void deleteUser(Integer userId);

    /**
     * 첫 로그인 시 추가 데이터 처리 (성별, 나이, 선호도 등)
     */
    UserDto processFirstLoginData(Integer userId, UserProfileDto profileDto);

    /**
     * 사용자 정보 조회
     */
    UserDto getUserById(Integer userId);

    /**
     * 프로필 완료 여부 확인 (첫 로그인 여부)
     */
    boolean isProfileComplete(Integer userId);
}