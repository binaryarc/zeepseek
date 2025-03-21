package com.zeepseek.backend.domain.auth.service;

import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.repository.UserRepository;
import com.zeepseek.backend.domain.auth.dto.response.UserResponse;
import com.zeepseek.backend.domain.auth.exception.CustomException;
import com.zeepseek.backend.domain.auth.exception.ErrorCode;
import com.zeepseek.backend.domain.auth.exception.ResourceNotFoundException;
import com.zeepseek.backend.domain.auth.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new ResourceNotFoundException("User", "nickname", nickname));
        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    @Override
    @Transactional
    public UserResponse updateUserInfo(Long userId, String nickname, Integer gender, Integer age) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 권한 확인 (본인 정보만 수정 가능)
        checkUserAuthorization(userId);

        // 닉네임 중복 체크
        if (nickname != null && !nickname.equals(user.getNickname()) && !isNicknameAvailable(nickname)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // 사용자 정보 업데이트
        user.updateUserInfo(nickname, gender, age);
        User updatedUser = userRepository.save(user);

        return UserResponse.from(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse toggleSellerStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 권한 확인 (본인 상태만 변경 가능)
        checkUserAuthorization(userId);

        // 판매자 상태 전환
        user.toggleSellerStatus();
        User updatedUser = userRepository.save(user);

        return UserResponse.from(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFirstLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return user.getIsFirst() == 1;
    }

    @Override
    @Transactional
    public void updateFirstLoginStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 첫 로그인 상태 업데이트
        if (user.getIsFirst() == 1) {
            user.markAsFirstLogin();
            userRepository.save(user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getSellers() {
        // 판매자 목록 조회 (isSeller = 1)
        List<User> sellers = userRepository.findAll().stream()
                .filter(user -> user.getIsSeller() == 1)
                .collect(Collectors.toList());

        return sellers.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        return securityUtils.getCurrentUser();
    }

    // 사용자 권한 확인 (본인 데이터만 수정 가능)
    private void checkUserAuthorization(Long userId) {
        User currentUser = securityUtils.getCurrentUser();
        if (!currentUser.getIdx().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "본인의 정보만 수정할 수 있습니다.");
        }
    }
}