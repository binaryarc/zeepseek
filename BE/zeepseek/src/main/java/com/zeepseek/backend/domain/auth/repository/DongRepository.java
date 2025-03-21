package com.zeepseek.backend.domain.auth.repository;

import com.zeepseek.backend.domain.auth.entity.Dong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DongRepository extends JpaRepository<Dong, Long> {

    // 동 이름으로 검색
    Optional<Dong> findByName(String name);

    // 특정 기준으로 정렬된 동 목록 조회
    // (예: 카페가 많은 동 순으로 정렬)
    List<Dong> findAllByOrderByCafeDesc();

    // (예: 안전도가 높은 동 순으로 정렬)
    List<Dong> findAllByOrderBySafeDesc();
}