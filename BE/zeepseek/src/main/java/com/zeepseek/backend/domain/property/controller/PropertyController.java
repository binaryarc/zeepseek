package com.zeepseek.backend.domain.property.controller;

import com.zeepseek.backend.domain.property.dto.PropertySummaryDto;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.service.PropertyService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/property")
public class PropertyController {

    private final PropertyService propertyService;

    @Autowired
    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // 매물 상세 조회 API: GET /api/v1/property/{propertyId}
    @GetMapping("/{propertyId}")
    public ResponseEntity<Property> getPropertyDetail(@PathVariable Long propertyId) {
        Property property = propertyService.getPropertyDetail(propertyId);
        if (property == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(property);
    }

    // 매물 전체 조회 API (모든 데이터, DTO로 변환): GET /api/v1/property/all
    @GetMapping("/all")
    public ResponseEntity<List<PropertySummaryDto>> getAllPropertySummaries() {
        List<PropertySummaryDto> summaries = propertyService.getAllPropertySummaries();
        return ResponseEntity.ok(summaries);
    }
}
