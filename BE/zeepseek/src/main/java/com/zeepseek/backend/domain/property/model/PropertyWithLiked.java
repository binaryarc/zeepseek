package com.zeepseek.backend.domain.property.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyWithLiked extends Property{

    boolean isLiked;

}
