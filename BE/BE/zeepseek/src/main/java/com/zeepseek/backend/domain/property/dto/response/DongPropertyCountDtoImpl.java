package com.zeepseek.backend.domain.property.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DongPropertyCountDtoImpl implements DongPropertyCountDto {
    private Integer dongId;
    private Long propertyCount;
}
