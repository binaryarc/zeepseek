package com.zeepseek.backend.domain.dong.controller;

import com.zeepseek.backend.domain.dong.document.DongCompareDocs;
import com.zeepseek.backend.domain.dong.document.PropertyCompareDocs;
import com.zeepseek.backend.domain.dong.service.CompareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dong/compare")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    @PostMapping("/dong")
    public ResponseEntity<?> compareDong(@RequestBody Map<String, Object> request) {
        int dong1 = (int) request.get("dong1");
        int dong2 = (int) request.get("dong2");

        DongCompareDocs response = compareService.compareDong(dong1,dong2);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/property")
    public ResponseEntity<?> compareProperty(@RequestBody Map<String, Object> request) {
        int prop1 = (int) request.get("prop1");
        int prop2 = (int) request.get("prop2");

        PropertyCompareDocs response = compareService.compareProperty(prop1,prop2);

        return ResponseEntity.ok(response);
    }
}
