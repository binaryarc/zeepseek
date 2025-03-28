package com.zeepseek.backend.domain.zzim.repository;

import com.zeepseek.backend.domain.zzim.document.DongZzimDoc;
import com.zeepseek.backend.domain.zzim.document.PropertyZzimDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PropertyZzimRepository extends MongoRepository<PropertyZzimDoc, Integer> {

    void deleteByUserIdAndPropertyId(int userId, int propertyId);

    List<PropertyZzimDoc> findAllByUserId(int userId);
}
