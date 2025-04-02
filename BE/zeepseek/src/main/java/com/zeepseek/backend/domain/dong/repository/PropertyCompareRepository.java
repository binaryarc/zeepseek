package com.zeepseek.backend.domain.dong.repository;

import com.zeepseek.backend.domain.dong.document.DongCompareDocs;
import com.zeepseek.backend.domain.dong.document.PropertyCompareDocs;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PropertyCompareRepository extends MongoRepository<PropertyCompareDocs, Integer> {
    PropertyCompareDocs findByCompareId(Integer compareId);
}
