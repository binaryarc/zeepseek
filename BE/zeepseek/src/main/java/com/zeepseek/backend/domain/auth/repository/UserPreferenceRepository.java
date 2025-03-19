package com.zeepseek.backend.domain.auth.repository;

import com.zeepseek.backend.domain.auth.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    // 사용자 ID로 선호도 정보 조회
    Optional<UserPreference> findByUserId(Long userId);
}