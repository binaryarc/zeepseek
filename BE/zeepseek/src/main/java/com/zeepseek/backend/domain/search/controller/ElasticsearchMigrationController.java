package com.zeepseek.backend.domain.search.controller;

import com.zeepseek.backend.domain.search.service.ElasticsearchDataMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ElasticsearchMigrationController {

    private final ElasticsearchDataMigrationService migrationService;

    @PostMapping("/migrate")
    public ResponseEntity<String> migrateData() {
        try {
            String result = migrationService.migrate();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Bulk 인덱싱 중 에러 발생: " + e.getMessage());
        }
    }
}
