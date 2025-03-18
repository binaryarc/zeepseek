package com.zeepseek.backend.domain.test;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestMySQLRepository testMySQLRepository;

    public void testinsert(String name, int age) {
        testMySQLRepository.save(TestMySQLEntity.builder().name(name).age(age).build());
    }


}
