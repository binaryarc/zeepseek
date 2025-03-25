package com.zeepseek.backend.domain.compare.repository;

import com.zeepseek.backend.domain.compare.entity.DongInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MySQLDongRepository extends JpaRepository<DongInfo, Integer> {
}
