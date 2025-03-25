package com.zeepseek.backend.domain.compare.repository;

import com.zeepseek.backend.domain.compare.document.DongInfoDocs;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoDongRepository extends MongoRepository<DongInfoDocs, Integer> {
}
