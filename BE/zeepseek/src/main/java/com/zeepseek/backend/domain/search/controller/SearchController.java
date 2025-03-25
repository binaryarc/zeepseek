package com.zeepseek.backend.domain.search.controller;

import com.zeepseek.backend.domain.search.dto.SearchProperty;
import com.zeepseek.backend.domain.search.dto.response.KeywordResponse;
import com.zeepseek.backend.domain.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    /**
     * GET /api/v1/property/search?keyword={keyword}&page={page}&size={size}
     * 쿼리 파라미터로 키워드, 페이지 번호, 페이지 사이즈를 받아 검색 결과를 반환합니다.
     */
    @PostMapping()
    public ResponseEntity<KeywordResponse> searchProperties(@RequestBody Map<String, Object> request) {
        String keyword = (String) request.get("keyword");
        int page = (int) request.get("page");
        int size = (int) request.get("size");

        KeywordResponse results = searchService.searchProperties(keyword, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/v1/search/mapper?guName={guName}&dongName={dongName}&page={page}&size={size}
     * 쿼리 파라미터를 Map으로 받아 해당 조건에 맞는 검색 결과를 반환합니다.
     */
    @PostMapping("/mapper")
    public ResponseEntity<KeywordResponse> searchPropertiesByGuAndDong(@RequestBody Map<String, Object> request) {
        String guName = (String) request.get("guName");
        String dongName = (String) request.get("dongName");
        int page = (int) request.get("page");
        int size = (int) request.get("size");

        KeywordResponse results = searchService.searchPropertiesByGuAndDong(guName, dongName, page, size);
        return ResponseEntity.ok(results);
    }
}
