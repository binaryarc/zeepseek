package com.zeepseek.backend.domain.compare.repository;

import com.zeepseek.backend.domain.compare.document.DongInfoDocs;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoDongRepository extends MongoRepository<DongInfoDocs, Integer> {
    List<DongInfoDocs> findByNameContainingIgnoreCase(String name);
}
