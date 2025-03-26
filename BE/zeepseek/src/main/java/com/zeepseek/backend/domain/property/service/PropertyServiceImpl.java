package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import com.zeepseek.backend.domain.property.exception.PropertyNotFoundException;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyServiceImpl implements PropertyService {

    private static final Logger logger = LoggerFactory.getLogger(PropertyServiceImpl.class);
    private final PropertyRepository propertyRepository;

    @Autowired
    public PropertyServiceImpl(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public Property getPropertyDetail(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Property not found with id: {}", id);
                    return new PropertyNotFoundException("Property with id " + id + " not found.");
                });
    }

    @Override
    public List<PropertySummaryDto> getAllPropertySummaries() {
        List<Property> properties = propertyRepository.findAll();
        if (!properties.isEmpty()) {
            logger.info("Found {} properties for summary", properties.size());
        }
        return properties.stream()
                .map(p -> new PropertySummaryDto(p.getPropertyId(), p.getLatitude(), p.getLongitude()))
                .collect(Collectors.toList());
    }

    @Override
    public Page<PropertySummaryDto> getPropertySummaries(Pageable pageable) {
        Page<Property> page = propertyRepository.findAll(pageable);
        if (!page.isEmpty()) {
            logger.info("Fetched page {} of properties", pageable.getPageNumber());
        }
        return page.map(p -> new PropertySummaryDto(p.getPropertyId(), p.getLatitude(), p.getLongitude()));
    }

    @Override
    public List<Property> getPropertiesByDong(Integer dongId) {
        List<Property> properties = propertyRepository.findByDongId(dongId);
        if (!properties.isEmpty()) {
            logger.info("Found {} properties for dongId {}", properties.size(), dongId);
        } else {
            logger.info("No properties found for dongId {}", dongId);
        }
        return properties;
    }

    @Override
    public List<Property> getPropertiesByGu(String guName) {
        List<Property> properties = propertyRepository.findByGuName(guName);
        logger.info("Found {} properties for guName {}", properties.size(), guName);
        return properties;
    }

    @Override
    public List<DongPropertyCountDto> countPropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countPropertiesByDong();
        if (!counts.isEmpty()) {
            logger.info("Counting properties by dong: {} records", counts.size());
        }
        return counts;
    }

    @Override
    public List<GuPropertyCountDto> countPropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countPropertiesByGu();
        if (!counts.isEmpty()) {
            logger.info("Counting properties by gu: {} records", counts.size());
        }
        return counts;
    }

    // 해당 동에 있는 원룸(혹은 1룸, 2룸)의 부동산 조회
    @Override
    public List<Property> getOneRoomPropertiesByDongId(Integer dongId) {
        List<Property> properties = propertyRepository.findOneRoomByDongId(dongId);
        if (!properties.isEmpty()) {
            logger.info("Found {} one-room properties for dongId {}", properties.size(), dongId);
        } else {
            logger.info("No one-room properties found for dongId {}", dongId);
        }
        return properties;
    }

    // 해당 동에 있는 빌라나 주택 부동산 조회
    @Override
    public List<Property> getHousePropertiesByDongId(Integer dongId) {
        List<Property> properties = propertyRepository.findHouseByDongId(dongId);
        if (!properties.isEmpty()) {
            logger.info("Found {} house properties for dongId {}", properties.size(), dongId);
        } else {
            logger.info("No house properties found for dongId {}", dongId);
        }
        return properties;
    }

    // 해당 동에 있는 오피스텔 부동산 조회
    @Override
    public List<Property> getOfficePropertiesByDongId(Integer dongId) {
        List<Property> properties = propertyRepository.findOfficeByDongId(dongId);
        if (!properties.isEmpty()) {
            logger.info("Found {} office properties for dongId {}", properties.size(), dongId);
        } else {
            logger.info("No office properties found for dongId {}", dongId);
        }
        return properties;
    }
}
