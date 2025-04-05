package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.request.CellBoundsDto;
import com.zeepseek.backend.domain.property.dto.response.CellPropertiesDto;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.model.PropertyWithLiked;
import com.zeepseek.backend.domain.property.repository.PropertyRepository;
import com.zeepseek.backend.domain.zzim.document.PropertyZzimDoc;
import com.zeepseek.backend.domain.zzim.service.ZzimService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyCellsServiceImpl implements PropertyCellsService {

    private static final Logger log = LoggerFactory.getLogger(PropertyCellsServiceImpl.class);
    private final PropertyRepository propertyRepository;
    private final ZzimService zzimService;

    @Override
    public List<CellPropertiesDto> getPropertiesForCells(List<CellBoundsDto> cells, String propertyType, int userId) {
        return cells.stream().map(cell -> {
            List<Property> properties;
            // propertyType에 따라 쿼리 메서드를 호출합니다.
            switch (propertyType.toLowerCase()) {
                case "one-room":
                    properties = propertyRepository.findOneRoomPropertiesInCell(
                            cell.getMinLng(), cell.getMinLat(), cell.getMaxLng(), cell.getMaxLat());
                    break;
                case "office":
                    properties = propertyRepository.findOfficePropertiesInCell(
                            cell.getMinLng(), cell.getMinLat(), cell.getMaxLng(), cell.getMaxLat());
                    break;
                case "house":
                    properties = propertyRepository.findHousePropertiesInCell(
                            cell.getMinLng(), cell.getMinLat(), cell.getMaxLng(), cell.getMaxLat());
                    break;
                case "all":
                default:
                    properties = propertyRepository.findPropertiesInCell(
                            cell.getMinLng(), cell.getMinLat(), cell.getMaxLng(), cell.getMaxLat());
                    break;
            }
            if(userId > 0) {
                try {
                    List<PropertyZzimDoc> propertyZzimDocs = zzimService.userSelectPropertyList(userId);
                    // 사용자 찜 목록의 propertyId들을 Set으로 수집
                    Set<Integer> likedPropertyIds = propertyZzimDocs.stream()
                            .map(PropertyZzimDoc::getPropertyId)
                            .collect(Collectors.toSet());

                    // 각 Property를 PropertyWithLiked로 변환하면서 찜 여부 설정
                    properties = properties.stream().map(property -> {
                        PropertyWithLiked propertyWithLiked = new PropertyWithLiked();
                        // 기존 Property의 모든 필드를 복사
                        BeanUtils.copyProperties(property, propertyWithLiked);
                        // 해당 propertyId가 찜 목록에 있으면 true, 없으면 false
                        propertyWithLiked.setLiked(likedPropertyIds.contains(property.getPropertyId()));
                        return propertyWithLiked;
                    }).collect(Collectors.toList());
                } catch (Exception e) {
                    log.error("그리드 레벨 liked 표시중 userId error. userId: {}", userId, e);
                }

            }

            return new CellPropertiesDto(cell, properties);
        }).collect(Collectors.toList());
    }
}
