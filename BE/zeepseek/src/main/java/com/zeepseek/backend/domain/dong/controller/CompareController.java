package com.zeepseek.backend.domain.dong.controller;

import com.zeepseek.backend.domain.dong.document.DongCompareDocs;
import com.zeepseek.backend.domain.dong.document.PropertyCompareDocs;
import com.zeepseek.backend.domain.dong.service.CompareService;
import com.zeepseek.backend.domain.logevent.annotation.Loggable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dong/compare")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    @PostMapping("/dong")
    @Loggable(action = "compare", type = "dong_compare")
    public ResponseEntity<?> compareDong(
            @RequestBody Map<String, Object> request,
            @CookieValue(value = "userId", defaultValue = "-1", required = false ) int userId,
            @CookieValue(value = "age", defaultValue = "-1", required = false ) int age,
            @CookieValue(value = "gender", defaultValue = "-1", required = false ) String gender) {
        int dong1 = (int) request.get("dong1");
        int dong2 = (int) request.get("dong2");

        DongCompareDocs response = compareService.compareDong(dong1,dong2);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/property")
    @Loggable(action = "compare", type = "property_compare")
    public ResponseEntity<?> compareProperty(
            @RequestBody Map<String, Object> request,
            @CookieValue(value = "userId", defaultValue = "-1", required = false ) int userId,
            @CookieValue(value = "age", defaultValue = "-1", required = false ) int age,
            @CookieValue(value = "gender", defaultValue = "-1", required = false ) String gender) {
        int prop1 = (int) request.get("prop1");
        int prop2 = (int) request.get("prop2");

        PropertyCompareDocs response = compareService.compareProperty(prop1,prop2);

        return ResponseEntity.ok(response);
    }
}
