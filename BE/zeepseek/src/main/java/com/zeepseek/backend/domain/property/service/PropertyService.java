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
    List<Property> getPropertiesByDong(Integer dongId);
    List<Property> getPropertiesByGu(String guName);
    List<DongPropertyCountDto> countPropertiesByDong();
    List<GuPropertyCountDto> countPropertiesByGu();

    // 해당 동에 있는 원룸(혹은 1룸, 2룸)의 부동산 조회
    List<Property> getOneRoomPropertiesByDongId(Integer dongId);

    // 해당 동에 있는 빌라나 주택 부동산 조회
    List<Property> getHousePropertiesByDongId(Integer dongId);

    // 해당 동에 있는 오피스텔 부동산 조회
    List<Property> getOfficePropertiesByDongId(Integer dongId);

    List<Property> getOneRoomPropertiesByGuName(String guName);
    List<Property> getHousePropertiesByGuName(String guName);
    List<Property> getOfficePropertiesByGuName(String guName);

    List<Property> getOneRoomProperties();
    List<Property> getHouseProperties();
    List<Property> getOfficeProperties();

    List<DongPropertyCountDto> countOneRoomPropertiesByDong();
    List<DongPropertyCountDto> countHousePropertiesByDong();
    List<DongPropertyCountDto> countOfficePropertiesByDong();
}
