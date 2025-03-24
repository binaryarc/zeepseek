package com.zeepseek.backend.domain.property.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertySummaryDto {
    private Long propertyId;
    private Double latitude;
    private Double longitude;
}
