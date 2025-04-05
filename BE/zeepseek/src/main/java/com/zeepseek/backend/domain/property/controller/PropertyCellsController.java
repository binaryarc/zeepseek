package com.zeepseek.backend.domain.property.controller;

import com.zeepseek.backend.domain.property.dto.request.PropertyCellsRequestDto;
import com.zeepseek.backend.domain.property.dto.response.CellPropertiesDto;
import com.zeepseek.backend.domain.property.service.PropertyCellsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/property")
@RequiredArgsConstructor
public class PropertyCellsController {

    private final PropertyCellsService propertyCellsService;

    /**
     * 여러 셀 범위에 대해 매물 목록을 반환합니다.
     * URL 예시: POST /api/v1/property/cells?type=one-room  
     * propertyType 값: "all", "one-room", "house", "office"
     */
    @PostMapping("/cells")
    public ResponseEntity<List<CellPropertiesDto>> getPropertiesForCells(
            @RequestBody PropertyCellsRequestDto requestDto,
            @RequestParam(defaultValue = "all") String type,
            @CookieValue(value = "userId", required = false , defaultValue = "-1") int userId) {
        List<CellPropertiesDto> response = propertyCellsService.getPropertiesForCells(requestDto.getCells(), type, userId);
        return ResponseEntity.ok(response);
    }
}
