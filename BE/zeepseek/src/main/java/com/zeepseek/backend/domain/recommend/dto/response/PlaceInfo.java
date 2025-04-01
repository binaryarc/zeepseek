package com.zeepseek.backend.domain.recommend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceInfo {
    private String name;
    private String latitude;
    private String longitude;
}
