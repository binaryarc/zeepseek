package com.zeepseek.backend.domain.dong.repository;

import com.zeepseek.backend.domain.dong.document.DongCompareDocs;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompareRepository extends MongoRepository<DongCompareDocs, Integer> {
    DongCompareDocs findByCompareId(Integer compareId);
}
