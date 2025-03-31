package com.zeepseek.backend.domain.zzim.service;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.repository.MongoDongRepository;
import com.zeepseek.backend.domain.dong.service.DongService;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.zzim.document.DongZzimDoc;
import com.zeepseek.backend.domain.zzim.document.PropertyZzimDoc;
import com.zeepseek.backend.domain.zzim.repository.DongZzimRepository;
import com.zeepseek.backend.domain.zzim.repository.PropertyZzimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZzimService {

    private final DongZzimRepository dongZzimRepository;
    private final PropertyZzimRepository propertyZzimRepository;
    private final DongService dongService;
    private final PropertyService propertyService;

    // 동네 찜 등록
    public ResponseEntity<?> zzimDong(int userId, int dongId) {
        try {
            dongZzimRepository.save(DongZzimDoc.builder().dongId(dongId).userId(userId).build());
            return ResponseEntity.ok("찜 등록 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("중복된 요청입니다.");
        }
    }

    // 매물 찜 등록
    public ResponseEntity<?> zzimProperty(int userId, int propertyId) {
        try {
            propertyZzimRepository.save(PropertyZzimDoc.builder().userId(userId).propertyId(propertyId).build());
            return ResponseEntity.ok("찜 등록 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("중복된 요청입니다.");
        }
    }
    
    // 동네 찜 삭제
    public ResponseEntity<?> deleteDongZzim(int userId, int dongId) {
        try {
            dongZzimRepository.deleteByUserIdAndDongId(userId, dongId);
            return ResponseEntity.ok("찜 삭제 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("이미 삭제된 요청입니다.");
        }
    }
    
    // 매물 찜 삭제
    public ResponseEntity<?> deletePropertyZzim(int userId, int propertyId) {
        try {
            propertyZzimRepository.deleteByUserIdAndPropertyId(userId, propertyId);
            return ResponseEntity.ok("찜 삭제 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("이미 삭제된 요청입니다.");
        }
    }
    // 유저별 동네 찜 리스트 반환
    public List<DongInfoDocs> selectDongZzimList(int userId) {

        List<DongZzimDoc> dongZzimDocs = dongZzimRepository.findAllByUserId(userId);

        List<DongInfoDocs> dongInfoDocs = new ArrayList<>();
        for(DongZzimDoc dongZzimDoc : dongZzimDocs) {
            DongInfoDocs docs = null;
            try {
                docs = dongService.getDongDetail(dongZzimDoc.getDongId());
            }catch (Exception e) {
                log.warn("찜: 해당 동네을 찾을 수 없습니다. {}", e);
            }
            dongInfoDocs.add(docs);
        }

        return dongInfoDocs;
    }

    // 유저별 매물 찜 리스트 반환
    public List<Property> selectPropertyZzimList(int userId) {

        List<PropertyZzimDoc> propertyZzimDocs = propertyZzimRepository.findAllByUserId(userId);

        List<Property> properties = new ArrayList<>();
        for(PropertyZzimDoc propertyZzimDoc : propertyZzimDocs) {
            Property property = null;
            try {
                property = propertyService.getPropertyDetail((long) propertyZzimDoc.getPropertyId());
            } catch (Exception e) {
                log.warn("찜: 해당 매물을 찾을 수 없습니다. {}", e);
            }
            properties.add(property);
        }

        return properties;
    }
}
