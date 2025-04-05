package com.zeepseek.backend.domain.distance.controller;

import com.zeepseek.backend.domain.distance.dto.request.CoordinateInfo;
import com.zeepseek.backend.domain.distance.dto.response.CoordinateResponse;
import com.zeepseek.backend.domain.distance.service.DistanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/distance")
public class DistanceController {

    private final DistanceService distanceService;

    @PostMapping("/coords")
    public ResponseEntity<CoordinateResponse> distance(@RequestBody CoordinateInfo coordinateInfo) {

        return ResponseEntity.ok(distanceService.haversineDistance(coordinateInfo));
    }

}
