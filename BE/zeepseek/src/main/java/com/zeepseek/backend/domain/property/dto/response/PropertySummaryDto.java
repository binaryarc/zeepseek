package com.zeepseek.backend.domain.property.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertySummaryDto {
    private Integer propertyId;
    private Double latitude;
    private Double longitude;
}
