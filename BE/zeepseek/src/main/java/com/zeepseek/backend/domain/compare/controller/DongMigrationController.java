package com.zeepseek.backend.domain.compare.controller;

import com.zeepseek.backend.domain.compare.service.DongMigrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mongodb")
public class DongMigrationController {

    private final DongMigrationService dongMigrationService;

    public DongMigrationController(DongMigrationService dongMigrationService) {
        this.dongMigrationService = dongMigrationService;
    }

    @PostMapping("/migration")
    public ResponseEntity<String> runMigration() {
        try {
            dongMigrationService.migrateAndUpdateDongData();
            return ResponseEntity.ok("마이그레이션 및 요약 업데이트가 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("마이그레이션 실행 중 오류 발생: " + e.getMessage());
        }
    }
}