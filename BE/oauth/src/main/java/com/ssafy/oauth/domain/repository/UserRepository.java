package com.ssafy.oauth.domain.repository;

import com.ssafy.oauth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 제공자와 제공자ID로 사용자 찾기 (OAuth 인증 후 사용자 조회 시 사용)
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // 닉네임으로 사용자 찾기
    Optional<User> findByNickname(String nickname);

    // 닉네임 중복 확인
    boolean existsByNickname(String nickname);

    // 특정 제공자와 제공자ID 조합이 존재하는지 확인 (중복 가입 방지)
    boolean existsByProviderAndProviderId(String provider, String providerId);

    // 리프레시 토큰으로 사용자 찾기 (토큰 갱신 시 사용)
    Optional<User> findByRefreshToken(String refreshToken);
}