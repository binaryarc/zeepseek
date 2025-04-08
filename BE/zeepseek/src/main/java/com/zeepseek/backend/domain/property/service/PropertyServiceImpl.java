package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.request.CellBoundsDto;
import com.zeepseek.backend.domain.property.dto.response.CellPropertiesDto;
import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDtoImpl;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDtoImpl;
import com.zeepseek.backend.domain.property.dto.response.PropertySummaryDto;
import com.zeepseek.backend.domain.property.exception.PropertyNotFoundException;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.model.PropertyScore;
import com.zeepseek.backend.domain.property.repository.PropertyRepository;
import com.zeepseek.backend.domain.property.repository.PropertyScoreRepository;
import com.zeepseek.backend.domain.ranking.service.RankingService;
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
    private final PropertyScoreRepository propertyScoreRepository;
    private final RankingService rankingService;

    @Autowired
    public PropertyServiceImpl(PropertyRepository propertyRepository,  PropertyScoreRepository propertyScoreRepository, RankingService rankingService) {
        this.propertyRepository = propertyRepository;
        this.propertyScoreRepository = propertyScoreRepository;
        this.rankingService = rankingService;
    }

    @Override
    public Property getPropertyDetail(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> {
                    // 존재하지 않으면 예외 처리
                    logger.warn("Property not found with id: {}", id);
                    return new PropertyNotFoundException("Property with id " + id + " not found.");
                });
        // property의 dongId와 propertyId를 이용해 ranking score 증가
        rankingService.incrementPropertyCount(property.getDongId(), property.getPropertyId());
        return property;
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
    @Cacheable(value = "dongCounts", key = "'all'", cacheManager = "propertyCacheManager")
    public List<DongPropertyCountDto> countPropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countPropertiesByDong();
        if (!counts.isEmpty()) {
            logger.info("Counting properties by dong: {} records", counts.size());
        }
        return counts.stream()
                .map(c -> new DongPropertyCountDtoImpl(c.getDongId(), c.getPropertyCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "guCounts", key = "'all'", cacheManager = "propertyCacheManager")
    public List<GuPropertyCountDto> countPropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countPropertiesByGu();
        if (!counts.isEmpty()) {
            logger.info("Counting properties by gu: {} records", counts.size());
        }
        return counts.stream()
                .map(c -> new GuPropertyCountDtoImpl(c.getGuName(), c.getPropertyCount()))
                .collect(Collectors.toList());
    }

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

    @Override
    public List<Property> getOneRoomPropertiesByGuName(String guName) {
        List<Property> properties = propertyRepository.findOneRoomByGuName(guName);
        logger.info("Found {} one-room properties for guName {}", properties.size(), guName);
        return properties;
    }

    @Override
    public List<Property> getHousePropertiesByGuName(String guName) {
        List<Property> properties = propertyRepository.findHouseByGuName(guName);
        logger.info("Found {} house properties for guName {}", properties.size(), guName);
        return properties;
    }

    @Override
    public List<Property> getOfficePropertiesByGuName(String guName) {
        List<Property> properties = propertyRepository.findOfficeByGuName(guName);
        logger.info("Found {} office properties for guName {}", properties.size(), guName);
        return properties;
    }

    @Override
    public List<Property> getOneRoomProperties() {
        List<Property> properties = propertyRepository.findOneRoomProperties();
        logger.info("Found {} one-room properties for all regions", properties.size());
        return properties;
    }

    @Override
    public List<Property> getHouseProperties() {
        List<Property> properties = propertyRepository.findHouseProperties();
        logger.info("Found {} house properties for all regions", properties.size());
        return properties;
    }

    @Override
    public List<Property> getOfficeProperties() {
        List<Property> properties = propertyRepository.findOfficeProperties();
        logger.info("Found {} office properties for all regions", properties.size());
        return properties;
    }

    @Override
    @Cacheable(value = "dongOneRoomCounts", key = "'all'", cacheManager = "propertyCacheManager")
    public List<DongPropertyCountDto> countOneRoomPropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countOneRoomPropertiesByDong();
        logger.info("Found one-room property counts for {} dong records", counts.size());
        return counts.stream()
                .map(c -> new DongPropertyCountDtoImpl(c.getDongId(), c.getPropertyCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "dongHouseCounts", key = "'all'", cacheManager = "propertyCacheManager")
    public List<DongPropertyCountDto> countHousePropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countHousePropertiesByDong();
        logger.info("Found house property counts for {} dong records", counts.size());
        return counts.stream()
                .map(c -> new DongPropertyCountDtoImpl(c.getDongId(), c.getPropertyCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "dongOfficeCounts", key = "'all'", cacheManager = "propertyCacheManager")
    public List<DongPropertyCountDto> countOfficePropertiesByDong() {
        List<DongPropertyCountDto> counts = propertyRepository.countOfficePropertiesByDong();
        logger.info("Found office property counts for {} dong records", counts.size());
        return counts.stream()
                .map(c -> new DongPropertyCountDtoImpl(c.getDongId(), c.getPropertyCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "guOneRoomCounts", key = "'all'", cacheManager = "propertyCacheManager")
    public List<GuPropertyCountDto> countOneRoomPropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countOneRoomPropertiesByGu();
        logger.info("Found one-room property counts for {} gu records", counts.size());
        return counts.stream()
                .map(c -> new GuPropertyCountDtoImpl(c.getGuName(), c.getPropertyCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "guHouseCounts", key = "'all'", cacheManager = "propertyCacheManager")
    public List<GuPropertyCountDto> countHousePropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countHousePropertiesByGu();
        logger.info("Found house property counts for {} gu records", counts.size());
        return counts.stream()
                .map(c -> new GuPropertyCountDtoImpl(c.getGuName(), c.getPropertyCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "guOfficeCounts", key = "'all'", cacheManager = "propertyCacheManager")
    public List<GuPropertyCountDto> countOfficePropertiesByGu() {
        List<GuPropertyCountDto> counts = propertyRepository.countOfficePropertiesByGu();
        logger.info("Found office property counts for {} gu records", counts.size());
        return counts.stream()
                .map(c -> new GuPropertyCountDtoImpl(c.getGuName(), c.getPropertyCount()))
                .collect(Collectors.toList());
    }

    @Override
    public PropertyScore getPropertyScoreByPropertyId(Integer propertyId) {
        return propertyScoreRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> {
                    logger.warn("PropertyScore not found for propertyId: {}", propertyId);
                    return new PropertyNotFoundException("Score not found for propertyId: " + propertyId);
                });
    }
}
