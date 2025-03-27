package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.dto.request.CellBoundsDto;
import com.zeepseek.backend.domain.property.dto.response.CellPropertiesDto;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyCellsServiceImpl implements PropertyCellsService {

    private final PropertyRepository propertyRepository;

    @Override
    public List<CellPropertiesDto> getPropertiesForCells(List<CellBoundsDto> cells, String propertyType) {
        return cells.stream().map(cell -> {
            List<Property> properties;
            // propertyType에 따라 쿼리 메서드를 호출합니다.
            switch (propertyType.toLowerCase()) {
                case "one-room":
                    properties = propertyRepository.findOneRoomPropertiesInCell(
                            cell.getMinLat(), cell.getMaxLat(), cell.getMinLng(), cell.getMaxLng());
                    break;
                case "office":
                    properties = propertyRepository.findOfficePropertiesInCell(
                            cell.getMinLat(), cell.getMaxLat(), cell.getMinLng(), cell.getMaxLng());
                    break;
                case "house":
                    properties = propertyRepository.findHousePropertiesInCell(
                            cell.getMinLat(), cell.getMaxLat(), cell.getMinLng(), cell.getMaxLng());
                    break;
                case "all":
                default:
                    properties = propertyRepository.findPropertiesInCell(
                            cell.getMinLat(), cell.getMaxLat(), cell.getMinLng(), cell.getMaxLng());
                    break;
            }
            return new CellPropertiesDto(cell, properties);
        }).collect(Collectors.toList());
    }
}
