// src/main/java/com/zeepseek/backend/domain/property/service/PropertyService.java
package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import com.zeepseek.backend.domain.property.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PropertyService {
    Property getPropertyDetail(Long id);
    List<PropertySummaryDto> getAllPropertySummaries();
    Page<PropertySummaryDto> getPropertySummaries(Pageable pageable);
    List<PropertySummaryDto> getPropertiesByDong(Integer dongId);
    List<DongPropertyCountDto> countPropertiesByDong();
    List<GuPropertyCountDto> countPropertiesByGu();
}
