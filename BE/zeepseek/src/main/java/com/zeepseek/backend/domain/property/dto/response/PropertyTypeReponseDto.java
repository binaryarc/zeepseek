package com.zeepseek.backend.domain.property.dto.response;

import com.zeepseek.backend.domain.property.model.Property;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PropertyTypeReponseDto {
    List<Property> propertyList;
}
