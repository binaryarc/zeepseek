package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    @Autowired
    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    // 매물 상세 조회
    public Property getPropertyDetail(Long id) {
        return propertyRepository.findById(id).orElse(null);
    }

    // 매물 전체 조회 (모든 데이터 반환, DTO 변환)
    public List<PropertySummaryDto> getAllPropertySummaries() {
        List<Property> properties = propertyRepository.findAll();
        return properties.stream()
                .map(p -> new PropertySummaryDto(p.getPropertyId(), p.getLatitude(), p.getLongitude()))
                .collect(Collectors.toList());
    }

    // 페이징 처리가 필요한 경우 별도 메서드 구현 가능
}
