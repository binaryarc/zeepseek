package com.zeepseek.backend.domain.property.controller;

import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.property.exception.PropertyNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/property")
public class PropertyController {

    private final PropertyService propertyService;

    @Autowired
    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // 매물 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<Property> getPropertyDetail(@PathVariable Long id) {
        Property property = propertyService.getPropertyDetail(id);
        return ResponseEntity.ok(property);
    }

    // 매물 전체 조회 (페이징)
    @GetMapping
    public ResponseEntity<List<PropertySummaryDto>> getPropertySummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PropertySummaryDto> properties = propertyService.getPropertySummaries(PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(properties);
    }

    // 전체 매물 조회 (모든 데이터; 주의: 데이터량이 많을 수 있음)
    @GetMapping("/all")
    public ResponseEntity<List<PropertySummaryDto>> getAllPropertySummaries() {
        List<PropertySummaryDto> properties = propertyService.getAllPropertySummaries();
        return ResponseEntity.ok(properties);
    }

    // 동별 매물 수 조회 API
    @GetMapping("/count/dong")
    public ResponseEntity<List<DongPropertyCountDto>> countPropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyService.countPropertiesByDong();
        return ResponseEntity.ok(counts);
    }

    // 구별 매물 수 조회 API
    @GetMapping("/count/gu")
    public ResponseEntity<List<GuPropertyCountDto>> countPropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyService.countPropertiesByGu();
        return ResponseEntity.ok(counts);
    }

    // 특정 동의 매물 전체 조회 API
    @GetMapping("/dong/{dongId}")
    public ResponseEntity<List<PropertySummaryDto>> getPropertiesByDong(@PathVariable Integer dongId) {
        List<PropertySummaryDto> properties = propertyService.getPropertiesByDong(dongId);
        return ResponseEntity.ok(properties);
    }

    // 특정 구(gu) 내 매물 조회: GET /api/v1/property/gu/{guName}
    @GetMapping("/gu/{guName}")
    public ResponseEntity<List<PropertySummaryDto>> getPropertiesByGu(@PathVariable String guName) {
        List<PropertySummaryDto> properties = propertyService.getPropertiesByGu(guName);
        return ResponseEntity.ok(properties);
    }

}
