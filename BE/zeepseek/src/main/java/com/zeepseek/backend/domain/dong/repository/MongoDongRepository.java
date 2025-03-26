package com.zeepseek.backend.domain.dong.repository;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoDongRepository extends MongoRepository<DongInfoDocs, Integer> {
    List<DongInfoDocs> findByNameContainingIgnoreCase(String name);
    // dongId는 유일해야 하므로, 이 메서드는 단일 객체를 반환하도록 설계합니다.
    DongInfoDocs findByDongId(Integer dongId);
}
