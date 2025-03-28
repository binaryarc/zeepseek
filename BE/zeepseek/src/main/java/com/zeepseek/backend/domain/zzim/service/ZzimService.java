package com.zeepseek.backend.domain.zzim.service;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.zzim.document.DongZzimDoc;
import com.zeepseek.backend.domain.zzim.document.PropertyZzimDoc;
import com.zeepseek.backend.domain.zzim.repository.DongZzimRepository;
import com.zeepseek.backend.domain.zzim.repository.PropertyZzimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ZzimService {

    private final DongZzimRepository dongZzimRepository;
    private final PropertyZzimRepository propertyZzimRepository;

    // 동네 찜 등록
    public String zzimDong(int userId, int dongId) {
        try {
            dongZzimRepository.save(DongZzimDoc.builder().dongId(dongId).userId(userId).build());
            return "동네 찜 등록 성공!";
        } catch (Exception e) {
            e.printStackTrace();
            return "동네 찜 등록 실패";
        }
    }

    // 매물 찜 등록
    public String zzimProperty(int userId, int propertyId) {
        try {
            propertyZzimRepository.save(PropertyZzimDoc.builder().userId(userId).propertyId(propertyId).build());
            return "매물 찜 등록 성공!";
        } catch (Exception e) {
            e.printStackTrace();
            return "매물 찜 등록 실패";
        }
    }
    
    // 동네 찜 삭제
    public String deleteDongZzim(int userId, int dongId) {
        try {
            dongZzimRepository.deleteByUserIdAndDongId(userId, dongId);
            return "매물 찜 삭제 성공!";
        } catch (Exception e) {
            e.printStackTrace();
            return "매물 찜 삭제 실패";
        }
    }
    
    // 매물 찜 삭제
    public String deletePropertyZzim(int userId, int propertyId) {
        try {
            propertyZzimRepository.deleteByUserIdAndPropertyId(userId, propertyId);
            return "매물 찜 삭제 성공!";
        } catch (Exception e) {
            e.printStackTrace();
            return "매물 찜 삭제 실패";
        }
    }
    // 유저별 동네 찜 리스트 반환
    public List<DongInfoDocs> selectDongZzimList(int userId) {

        List<DongZzimDoc> dongZzimDocs = dongZzimRepository.findAllByUserId(userId);

        List<DongInfoDocs> dongInfoDocs = new ArrayList<>();
        for(DongZzimDoc dongZzimDoc : dongZzimDocs) {

        }

        return null;
    }

    
    // 유저별 매물 찜 리스트 반환

}
