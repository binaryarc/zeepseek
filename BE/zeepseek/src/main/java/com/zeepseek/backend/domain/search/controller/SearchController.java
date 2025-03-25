package com.zeepseek.backend.domain.search.controller;

import com.zeepseek.backend.domain.search.dto.SearchProperty;
import com.zeepseek.backend.domain.search.dto.response.KeywordResponse;
import com.zeepseek.backend.domain.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    /**
     * GET /api/v1/property/search?keyword={keyword}&page={page}&size={size}
     * 쿼리 파라미터로 키워드, 페이지 번호, 페이지 사이즈를 받아 검색 결과를 반환합니다.
     */
    @GetMapping()
    public ResponseEntity<KeywordResponse> searchProperties(
            @RequestParam("keyword") String keyword,
            @RequestParam("page") int page,
            @RequestParam("size") int size) {

        KeywordResponse results = searchService.searchProperties(keyword, page, size);
        return ResponseEntity.ok(results);
    }
}
