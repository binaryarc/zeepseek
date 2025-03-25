package com.zeepseek.backend.domain.compare.controller;

import com.zeepseek.backend.domain.compare.document.DongInfoDocs;
import com.zeepseek.backend.domain.compare.service.DongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dong")
@RequiredArgsConstructor
public class DongController {

    private final DongService dongService;

    /**
     * 동 이름으로 검색하는 API
     * 요청 예: GET /api/dongs/search?name=신당
     */
    @GetMapping("/search")
    public ResponseEntity<List<DongInfoDocs>> searchDongs(@RequestParam("name") String name) {
        List<DongInfoDocs> dongs = dongService.searchByName(name);
        return ResponseEntity.ok(dongs);
    }

    /**
     * dongId로 특정 동의 상세 정보를 조회하는 API
     * 요청 예: GET /api/dongs/123
     */
    @GetMapping("/{dongId}")
    public ResponseEntity<DongInfoDocs> getDongDetail(@PathVariable("dongId") Integer dongId) {
        DongInfoDocs dong = dongService.getDongDetail(dongId);
        return dong != null ? ResponseEntity.ok(dong) : ResponseEntity.notFound().build();
    }
}
