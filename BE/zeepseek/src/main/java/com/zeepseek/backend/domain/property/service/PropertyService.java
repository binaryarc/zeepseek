package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.request.CellBoundsDto;
import com.zeepseek.backend.domain.property.dto.response.CellPropertiesDto;
import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.model.PropertyScore;
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
    List<Property> getOneRoomPropertiesByDongId(Integer dongId);
    List<Property> getHousePropertiesByDongId(Integer dongId);
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
    List<GuPropertyCountDto> countOneRoomPropertiesByGu();
    List<GuPropertyCountDto> countHousePropertiesByGu();
    List<GuPropertyCountDto> countOfficePropertiesByGu();

    PropertyScore getPropertyScoreByPropertyId(Integer propertyId);
}
