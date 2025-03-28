package com.zeepseek.backend.domain.zzim.controller;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.service.DongService;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.zzim.service.ZzimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/zzim")
@RequiredArgsConstructor
public class ZzimController {

    private final ZzimService zzimService;

    // 동네 찜
    @PostMapping("/dong/{dongId}")
    public ResponseEntity<?> dongZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "dongId") int dongId) {

        return zzimService.zzimDong(userId, dongId);
    }

    // 매물 찜
    @PostMapping("/property/{propertyId}")
    public ResponseEntity<?> propertyZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "propertyId") int propertyId) {

        return zzimService.zzimProperty(userId, propertyId);
    }

    // 동네 찜 삭제
    @DeleteMapping("/dong/{dongId}")
    public ResponseEntity<?> deleteDongZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "dongId") int dongId) {

        return zzimService.deleteDongZzim(userId, dongId);
    }

    // 매물 찜 삭제
    @DeleteMapping("/property/{propertyId}")
    public ResponseEntity<?> deletePropertyZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "propertyId") int propertyId) {

        return zzimService.deletePropertyZzim(userId, propertyId);
    }

    @GetMapping("/select/dong")
    public ResponseEntity<?> selectAllDongZzim(@CookieValue(value = "userId", defaultValue = "") int userId) {

        List<DongInfoDocs> response = zzimService.selectDongZzimList(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/select/property")
    public ResponseEntity<?> selectAllPropertyZzim(@CookieValue(value = "userId", defaultValue = "") int userId) {

        List<Property> response = zzimService.selectPropertyZzimList(userId);

        return ResponseEntity.ok(response);
    }

}
