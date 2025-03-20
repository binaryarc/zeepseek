package com.zeepseek.backend.domain.test;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TestController {

    private final TestService testService;

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok("여기는 ec2 응답하라!");
    }

    @PostMapping("/test")
    public ResponseEntity<?> testsave(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        int age = (int) request.get("age");

        testService.testinsert(name,age);

        return ResponseEntity.status(200).body("테스트 삽입 성공");
    }
}
