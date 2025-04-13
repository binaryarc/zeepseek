package com.zeepseek.backend.domain.dong.repository;

import com.zeepseek.backend.domain.dong.entity.DongInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MySQLDongRepository extends JpaRepository<DongInfo, Integer> {
    Optional<DongInfo> findByName(String name);
}
