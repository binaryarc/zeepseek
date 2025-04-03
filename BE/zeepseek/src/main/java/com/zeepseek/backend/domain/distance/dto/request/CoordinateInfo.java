package com.zeepseek.backend.domain.distance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinateInfo {
    double lat1;
    double lon1;

    double lat2;
    double lon2;
}
