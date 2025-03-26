package com.zeepseek.backend.domain.dong.repository;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoDongRepository extends MongoRepository<DongInfoDocs, Integer> {
    List<DongInfoDocs> findByNameContainingIgnoreCase(String name);
    DongInfoDocs findByDongId(Integer dongId);
}
