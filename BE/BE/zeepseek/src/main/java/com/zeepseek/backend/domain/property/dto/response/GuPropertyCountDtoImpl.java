package com.zeepseek.backend.domain.property.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuPropertyCountDtoImpl implements GuPropertyCountDto {
    private String guName;
    private Long propertyCount;
}
