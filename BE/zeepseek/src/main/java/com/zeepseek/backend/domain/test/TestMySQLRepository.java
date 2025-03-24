package com.zeepseek.backend.domain.test;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface TestMySQLRepository extends  JpaRepository<TestMySQLEntity, Integer>{

}
