package com.zeepseek.backend.domain.property.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyCellsRequestDto {
    private List<CellBoundsDto> cells;
}
