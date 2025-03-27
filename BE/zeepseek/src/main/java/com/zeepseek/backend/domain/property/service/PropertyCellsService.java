package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.request.CellBoundsDto;
import com.zeepseek.backend.domain.property.dto.response.CellPropertiesDto;

import java.util.List;

public interface PropertyCellsService {
    /**
     * 여러 셀에 대해 propertyType에 따라 필터링된 매물 목록을 반환
     * propertyType 값: "all", "one-room", "house", "office"
     */
    List<CellPropertiesDto> getPropertiesForCells(List<CellBoundsDto> cells, String propertyType);
}
