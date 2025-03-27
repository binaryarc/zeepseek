package com.zeepseek.backend.domain.property.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CellBoundsDto {
    private double minLat;
    private double maxLat;
    private double minLng;
    private double maxLng;
}
