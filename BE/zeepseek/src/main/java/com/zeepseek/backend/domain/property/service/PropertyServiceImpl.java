package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.request.CellBoundsDto;
import com.zeepseek.backend.domain.property.dto.response.CellPropertiesDto;
import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import com.zeepseek.backend.domain.property.exception.PropertyNotFoundException;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

    /**
     * 매물 상세 정보를 조회합니다.
     * 주어진 ID로 Property를 검색하며, 없으면 PropertyNotFoundException을 발생시킵니다.
     */
    @Override
    public Property getPropertyDetail(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Property not found with id: {}", id);
                    return new PropertyNotFoundException("Property with id " + id + " not found.");
                });
    }

    /**
     * 전체 매물 요약 정보를 조회합니다.
     * 각 Property를 PropertySummaryDto로 변환하여 매물 ID, 위도, 경도 정보를 반환합니다.
     */
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

    /**
     * 페이지 단위의 매물 요약 정보를 조회합니다.
     * Pageable을 이용해 Property 목록을 가져오고, 각 Property를 PropertySummaryDto로 변환합니다.
     */
    @Override
    public Page<PropertySummaryDto> getPropertySummaries(Pageable pageable) {
        Page<Property> page = propertyRepository.findAll(pageable);
        if (!page.isEmpty()) {
            logger.info("Fetched page {} of properties", pageable.getPageNumber());
        }
        return page.map(p -> new PropertySummaryDto(p.getPropertyId(), p.getLatitude(), p.getLongitude()));
    }

    /**
     * 특정 동(dongId)에 속한 매물 정보를 조회합니다.
     */
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

    /**
     * 특정 구(guName)에 속한 매물 정보를 조회합니다.
     */
    @Override
    public List<Property> getPropertiesByGu(String guName) {
        List<Property> properties = propertyRepository.findByGuName(guName);
        logger.info("Found {} properties for guName {}", properties.size(), guName);
        return properties;
    }

    /**
     * 동별 전체 매물 수를 집계하여 반환합니다.
     */
    @Override
    @Cacheable(value = "dongCounts", key = "'all'")
    public List<DongPropertyCountDto> countPropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countPropertiesByDong();
        if (!counts.isEmpty()) {
            logger.info("Counting properties by dong: {} records", counts.size());
        }
        return counts;
    }

    /**
     * 구별 전체 매물 수를 집계하여 반환합니다.
     */
    @Override
    @Cacheable(value = "guCounts", key = "'all'")
    public List<GuPropertyCountDto> countPropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countPropertiesByGu();
        if (!counts.isEmpty()) {
            logger.info("Counting properties by gu: {} records", counts.size());
        }
        return counts;
    }

    /**
     * 특정 동에 속한 원룸(혹은 1룸, 2룸) 매물을 조회합니다.
     */
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

    /**
     * 특정 동에 속한 빌라 또는 주택 매물을 조회합니다.
     */
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

    /**
     * 특정 동에 속한 오피스텔 매물을 조회합니다.
     */
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

    /**
     * 특정 구에 속한 원룸(혹은 1룸, 2룸) 매물을 조회합니다.
     */
    @Override
    public List<Property> getOneRoomPropertiesByGuName(String guName) {
        List<Property> properties = propertyRepository.findOneRoomByGuName(guName);
        logger.info("Found {} one-room properties for guName {}", properties.size(), guName);
        return properties;
    }

    /**
     * 특정 구에 속한 빌라 또는 주택 매물을 조회합니다.
     */
    @Override
    public List<Property> getHousePropertiesByGuName(String guName) {
        List<Property> properties = propertyRepository.findHouseByGuName(guName);
        logger.info("Found {} house properties for guName {}", properties.size(), guName);
        return properties;
    }

    /**
     * 특정 구에 속한 오피스텔 매물을 조회합니다.
     */
    @Override
    public List<Property> getOfficePropertiesByGuName(String guName) {
        List<Property> properties = propertyRepository.findOfficeByGuName(guName);
        logger.info("Found {} office properties for guName {}", properties.size(), guName);
        return properties;
    }

    /**
     * 전체 원룸 매물을 조회합니다.
     */
    @Override
    public List<Property> getOneRoomProperties() {
        List<Property> properties = propertyRepository.findOneRoomProperties();
        logger.info("Found {} one-room properties for all regions", properties.size());
        return properties;
    }

    /**
     * 전체 빌라/주택 매물을 조회합니다.
     */
    @Override
    public List<Property> getHouseProperties() {
        List<Property> properties = propertyRepository.findHouseProperties();
        logger.info("Found {} house properties for all regions", properties.size());
        return properties;
    }

    /**
     * 전체 오피스텔 매물을 조회합니다.
     */
    @Override
    public List<Property> getOfficeProperties() {
        List<Property> properties = propertyRepository.findOfficeProperties();
        logger.info("Found {} office properties for all regions", properties.size());
        return properties;
    }

    /**
     * 동별 원룸 매물 개수를 집계합니다.
     */
    @Override
    @Cacheable(value = "dongOneRoomCounts", key = "'all'")
    public List<DongPropertyCountDto> countOneRoomPropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countOneRoomPropertiesByDong();
        logger.info("Found one-room property counts for {} dong records", counts.size());
        return counts;
    }

    /**
     * 동별 빌라/주택 매물 개수를 집계합니다.
     */
    @Override
    @Cacheable(value = "dongHouseCounts", key = "'all'")
    public List<DongPropertyCountDto> countHousePropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countHousePropertiesByDong();
        logger.info("Found house property counts for {} dong records", counts.size());
        return counts;
    }

    /**
     * 동별 오피스텔 매물 개수를 집계합니다.
     */
    @Override
    @Cacheable(value = "dongOfficeCounts", key = "'all'")
    public List<DongPropertyCountDto> countOfficePropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countOfficePropertiesByDong();
        logger.info("Found office property counts for {} dong records", counts.size());
        return counts;
    }

    /**
     * 구별 원룸 매물 개수를 집계합니다.
     */
    @Override
    @Cacheable(value = "guOneRoomCounts", key = "'all'")
    public List<GuPropertyCountDto> countOneRoomPropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countOneRoomPropertiesByGu();
        logger.info("Found one-room property counts for {} gu records", counts.size());
        return counts;
    }

    /**
     * 구별 빌라/주택 매물 개수를 집계합니다.
     */
    @Override
    @Cacheable(value = "guHouseCounts", key = "'all'")
    public List<GuPropertyCountDto> countHousePropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countHousePropertiesByGu();
        logger.info("Found house property counts for {} gu records", counts.size());
        return counts;
    }

    /**
     * 구별 오피스텔 매물 개수를 집계합니다.
     */
    @Override
    @Cacheable(value = "guOfficeCounts", key = "'all'")
    public List<GuPropertyCountDto> countOfficePropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countOfficePropertiesByGu();
        logger.info("Found office property counts for {} gu records", counts.size());
        return counts;
    }
}
