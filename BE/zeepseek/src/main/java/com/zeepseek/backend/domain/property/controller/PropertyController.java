package com.zeepseek.backend.domain.property.controller;

import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.recommend.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/property")
public class PropertyController {

    private static final Logger logger = LoggerFactory.getLogger(PropertyController.class);

    private final PropertyService propertyService;


    @Autowired
    public PropertyController(PropertyService propertyService, RecommendationService recommendationService) {
        this.propertyService = propertyService;
    }

    // 매물 상세 조회 API: GET /api/v1/property/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Property> getPropertyDetail(@PathVariable Long id) {
        Property property = propertyService.getPropertyDetail(id);
        return ResponseEntity.ok(property);
    }

    // 매물 전체 조회 (페이징 처리): GET /api/v1/property?page=0&size=10
    @GetMapping
    public ResponseEntity<List<PropertySummaryDto>> getPropertySummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PropertySummaryDto> properties = propertyService.getPropertySummaries(PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(properties);
    }

    // 전체 매물 조회 (모든 데이터 반환; 주의: 데이터량이 많을 수 있음)
    @GetMapping("/all")
    public ResponseEntity<List<PropertySummaryDto>> getAllPropertySummaries() {
        List<PropertySummaryDto> properties = propertyService.getAllPropertySummaries();
        return ResponseEntity.ok(properties);
    }

    // 동별 매물 수 조회 API: GET /api/v1/property/count/dong
    @GetMapping("/count/dong")
    public ResponseEntity<List<DongPropertyCountDto>> countPropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyService.countPropertiesByDong();
        return ResponseEntity.ok(counts);
    }

    // 구별 매물 수 조회 API: GET /api/v1/property/count/gu
    @GetMapping("/count/gu")
    public ResponseEntity<List<GuPropertyCountDto>> countPropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyService.countPropertiesByGu();
        return ResponseEntity.ok(counts);
    }

    // 특정 동의 매물 전체 조회 API: GET /api/v1/property/dong/{dongId}
    @GetMapping("/dong/{dongId}")
    public ResponseEntity<List<PropertySummaryDto>> getPropertiesByDong(@PathVariable Integer dongId) {
        List<PropertySummaryDto> properties = propertyService.getPropertiesByDong(dongId);
        return ResponseEntity.ok(properties);
    }

    // 특정 구의 매물 조회 API: GET /api/v1/property/gu/{guName}
    @GetMapping("/gu/{guName}")
    public ResponseEntity<List<PropertySummaryDto>> getPropertiesByGu(@PathVariable String guName) {
        List<PropertySummaryDto> properties = propertyService.getPropertiesByGu(guName);
        return ResponseEntity.ok(properties);
    }

    // 추천 API: POST /api/v1/property/recommend?page=0&size=5
    // 사용자 카테고리 점수를 받아 Python FastAPI 추천 시스템과 연동하여 추천 매물 목록을 페이지네이션 처리해서 반환
}
