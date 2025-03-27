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
    public ResponseEntity<List<Property>> getPropertiesByDong(@PathVariable Integer dongId) {
        List<Property> properties = propertyService.getPropertiesByDong(dongId);
        return ResponseEntity.ok(properties);
    }

    // 특정 구의 매물 조회 API: GET /api/v1/property/gu/{guName}
    @GetMapping("/gu/{guName}")
    public ResponseEntity<List<Property>> getPropertiesByGu(@PathVariable String guName) {
        List<Property> properties = propertyService.getPropertiesByGu(guName);
        return ResponseEntity.ok(properties);
    }

    // 특정 동의 원룸(혹은 1룸, 2룸) 매물 조회 API: GET /api/v1/property/dong/{dongId}/one-room
    @GetMapping("/dong/{dongId}/one-room")
    public ResponseEntity<List<Property>> getOneRoomPropertiesByDong(@PathVariable Integer dongId) {
        List<Property> properties = propertyService.getOneRoomPropertiesByDongId(dongId);
        return ResponseEntity.ok(properties);
    }

    // 특정 동의 빌라 및 주택 매물 조회 API: GET /api/v1/property/dong/{dongId}/house
    @GetMapping("/dong/{dongId}/house")
    public ResponseEntity<List<Property>> getHousePropertiesByDong(@PathVariable Integer dongId) {
        List<Property> properties = propertyService.getHousePropertiesByDongId(dongId);
        return ResponseEntity.ok(properties);
    }

    // 특정 동의 오피스텔 매물 조회 API: GET /api/v1/property/dong/{dongId}/office
    @GetMapping("/dong/{dongId}/office")
    public ResponseEntity<List<Property>> getOfficePropertiesByDong(@PathVariable Integer dongId) {
        List<Property> properties = propertyService.getOfficePropertiesByDongId(dongId);
        return ResponseEntity.ok(properties);
    }

    // 특정 구의 원룸(혹은 1룸, 2룸) 매물 조회 API: GET /api/v1/property/gu/{guName}/one-room
    @GetMapping("/gu/{guName}/one-room")
    public ResponseEntity<List<Property>> getOneRoomPropertiesByGu(@PathVariable String guName) {
        List<Property> properties = propertyService.getOneRoomPropertiesByGuName(guName);
        return ResponseEntity.ok(properties);
    }

    // 특정 구의 빌라 및 주택 매물 조회 API: GET /api/v1/property/gu/{guName}/house
    @GetMapping("/gu/{guName}/house")
    public ResponseEntity<List<Property>> getHousePropertiesByGu(@PathVariable String guName) {
        List<Property> properties = propertyService.getHousePropertiesByGuName(guName);
        return ResponseEntity.ok(properties);
    }

    // 특정 구의 오피스텔 매물 조회 API: GET /api/v1/property/gu/{guName}/office
    @GetMapping("/gu/{guName}/office")
    public ResponseEntity<List<Property>> getOfficePropertiesByGu(@PathVariable String guName) {
        List<Property> properties = propertyService.getOfficePropertiesByGuName(guName);
        return ResponseEntity.ok(properties);
    }


    // --- 전체 구/동 기준 타입별 조회 (신규 추가) ---

    // 전체 원룸 매물 조회 API: GET /api/v1/property/type/one-room
    @GetMapping("/type/one-room")
    public ResponseEntity<List<Property>> getAllOneRoomProperties() {
        List<Property> properties = propertyService.getOneRoomProperties();
        return ResponseEntity.ok(properties);
    }

    // 전체 빌라/주택 매물 조회 API: GET /api/v1/property/type/house
    @GetMapping("/type/house")
    public ResponseEntity<List<Property>> getAllHouseProperties() {
        List<Property> properties = propertyService.getHouseProperties();
        return ResponseEntity.ok(properties);
    }

    // 전체 오피스텔 매물 조회 API: GET /api/v1/property/type/office
    @GetMapping("/type/office")
    public ResponseEntity<List<Property>> getAllOfficeProperties() {
        List<Property> properties = propertyService.getOfficeProperties();
        return ResponseEntity.ok(properties);
    }

    @GetMapping("/count/dong/one-room")
    public ResponseEntity<List<DongPropertyCountDto>> countOneRoomPropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyService.countOneRoomPropertiesByDong();
        return ResponseEntity.ok(counts);
    }

    // 동별 빌라/주택 매물 개수 조회: GET /api/v1/property/count/dong/house
    @GetMapping("/count/dong/house")
    public ResponseEntity<List<DongPropertyCountDto>> countHousePropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyService.countHousePropertiesByDong();
        return ResponseEntity.ok(counts);
    }

    // 동별 오피스텔 매물 개수 조회: GET /api/v1/property/count/dong/office
    @GetMapping("/count/dong/office")
    public ResponseEntity<List<DongPropertyCountDto>> countOfficePropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyService.countOfficePropertiesByDong();
        return ResponseEntity.ok(counts);
    }
}
