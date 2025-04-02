package com.zeepseek.backend.domain.zzim.controller;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.logevent.annotation.Loggable;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.zzim.document.DongZzimDoc;
import com.zeepseek.backend.domain.zzim.document.PropertyZzimDoc;
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

//    // 동네 찜
//    @Loggable(action = "zzim")
//    @PostMapping("/dong/{dongId}")
//    public ResponseEntity<?> dongZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "dongId") int dongId) {
//
//        return zzimService.zzimDong(userId, dongId);
//    }
//
//    // 매물 찜
//    @Loggable(action = "zzim")
//    @PostMapping("/property/{propertyId}")
//    public ResponseEntity<?> propertyZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "propertyId") int propertyId) {
//
//        return zzimService.zzimProperty(userId, propertyId);
//    }
//
//    // 동네 찜 삭제
//    @DeleteMapping("/dong/{dongId}")
//    public ResponseEntity<?> deleteDongZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "dongId") int dongId) {
//
//        return zzimService.deleteDongZzim(userId, dongId);
//    }
//
//    // 매물 찜 삭제
//    @DeleteMapping("/property/{propertyId}")
//    public ResponseEntity<?> deletePropertyZzim(@CookieValue(value = "userId", defaultValue = "") int userId, @PathVariable(name = "propertyId") int propertyId) {
//
//        return zzimService.deletePropertyZzim(userId, propertyId);
//    }

    // 동네 찜
    @PostMapping("/dong/{dongId}/{userId}")
    public ResponseEntity<?> dongZzim(@PathVariable("userId") int userId, @PathVariable("dongId") int dongId) {
        return zzimService.zzimDong(userId, dongId);
    }

    // 매물 찜
    @Loggable(action = "zzim", type = "dong")
    @PostMapping("/property/{propertyId}/{userId}")
    public ResponseEntity<?> propertyZzim(
            @PathVariable("userId") int userId,
            @PathVariable("propertyId") int propertyId,
            @CookieValue(name = "age", defaultValue = "-1", required = false ) int age,
            @CookieValue(name = "gender", defaultValue = "-1", required = false ) String gender) {
        return zzimService.zzimProperty(userId, propertyId);
    }

    // 동네 찜 삭제
    @DeleteMapping("/dong/{dongId}/{userId}")
    public ResponseEntity<?> deleteDongZzim(@PathVariable("userId") int userId, @PathVariable("dongId") int dongId) {
        return zzimService.deleteDongZzim(userId, dongId);
    }

    // 매물 찜 삭제
    @Loggable(action = "zzim_delete", type = "property")
    @DeleteMapping("/property/{propertyId}/{userId}")
    public ResponseEntity<?> deletePropertyZzim(
            @PathVariable("userId") int userId,
            @PathVariable("propertyId") int propertyId,
            @CookieValue(name = "age", defaultValue = "-1", required = false ) int age,
            @CookieValue(name = "gender", defaultValue = "-1", required = false ) String gender) {
        return zzimService.deletePropertyZzim(userId, propertyId);
    }

//    @GetMapping("/select/dong")
//    public ResponseEntity<?> selectAllDongZzim(@CookieValue(value = "userId", defaultValue = "") int userId) {
//
//        List<DongInfoDocs> response = zzimService.selectDongZzimList(userId);
//
//        return ResponseEntity.ok(response);
//    }

    // 유저의 찜한 동네정보 리스트로 조회
    @GetMapping("/select/dong/{userId}")
    public ResponseEntity<?> selectAllDongZzim(@PathVariable(name = "userId") int userId) {

        List<DongInfoDocs> response = zzimService.selectDongZzimList(userId);

        return ResponseEntity.ok(response);
    }

//    @GetMapping("/select/property")
//    public ResponseEntity<?> selectAllPropertyZzim(@CookieValue(value = "userId", defaultValue = "") int userId) {
//
//        List<Property> response = zzimService.selectPropertyZzimList(userId);
//
//        return ResponseEntity.ok(response);
//    }

    // 유저가 찜한 매물 정보 리스트로 조회
    @GetMapping("/select/property/{userId}")
    public ResponseEntity<?> selectAllPropertyZzim(@PathVariable(name = "userId") int userId) {

        List<Property> response = zzimService.selectPropertyZzimList(userId);

        return ResponseEntity.ok(response);
    }

    // 유저가 찜한 동네 id만 리스트로 조회
    @GetMapping("/select/dong_zzimid/{userId}")
    public ResponseEntity<?> selectAllDongZzimId(@PathVariable(name = "userId") int userId) {

        List<DongZzimDoc> response = zzimService.userSelectDongList(userId);

        return ResponseEntity.ok(response);
    }

    // 유저가 찜한 매물 id만 리스트로 조회
    @GetMapping("/select/property_zzimid/{userId}")
    public ResponseEntity<?> selectAllPropertyZzimId(@PathVariable(name = "userId") int userId) {

        List<PropertyZzimDoc> response = zzimService.userSelectPropertyList(userId);

        return ResponseEntity.ok(response);
    }

//    @GetMapping("/select/all_dong")
//    public ResponseEntity<?> selectAllDongs(@CookieValue(value = "userId", defaultValue = "") int userId) {
//
//        return ResponseEntity.ok(zzimService.findAllDongLiked(userId));
//    }

    // 유저가 찜했는지 안했는지 (isLiked) 여부를 포함한 동네 id 리스트를 조회
    @GetMapping("/select/all_dong/{userId}")
    public ResponseEntity<?> selectAllDongs(@PathVariable(name = "userId") int userId) {

        return ResponseEntity.ok(zzimService.findAllDongLiked(userId));
    }
}
