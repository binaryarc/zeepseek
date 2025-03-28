package com.zeepseek.backend.domain.zzim.controller;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.service.DongService;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.zzim.service.ZzimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/zzim")
@RequiredArgsConstructor
public class ZzimController {

    private final PropertyService propertyService;
    private final DongService dongService;
    private final ZzimService zzimService;

    // 동네 찜
    @PostMapping("/dong/{dongId}")
    public ResponseEntity<?> dongZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "dongId") int dongId) {



        return ResponseEntity.ok(200);
    }

    // 매물 찜
    @PostMapping("/property")
    public ResponseEntity<?> propertyZzim(@CookieValue(value = "userId", defaultValue = "") int userId) {



        return ResponseEntity.ok(200);
    }
}
