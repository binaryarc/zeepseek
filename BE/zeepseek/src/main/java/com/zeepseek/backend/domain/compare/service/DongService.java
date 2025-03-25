package com.zeepseek.backend.domain.compare.service;

import com.zeepseek.backend.domain.compare.document.DongInfoDocs;
import com.zeepseek.backend.domain.compare.repository.MongoDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DongService {

    private final MongoDongRepository dongRepository;

    /**
     * 동 이름을 포함한 검색 결과 반환
     * 예: "신당"을 검색하면 "신당동", "신당리" 등 관련 문서 반환
     */
    public List<DongInfoDocs> searchByName(String name) {
        // findByNameContainingIgnoreCase 메서드는 MongoDongRepository에 미리 정의되어 있다고 가정합니다.
        return dongRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * dongId를 기반으로 해당 동의 상세 정보를 반환
     */
    public DongInfoDocs getDongDetail(Integer dongId) {
        return dongRepository.findById(dongId).orElse(null);
    }
}