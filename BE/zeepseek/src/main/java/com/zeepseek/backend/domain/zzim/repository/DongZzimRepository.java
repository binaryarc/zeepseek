package com.zeepseek.backend.domain.zzim.repository;

import com.zeepseek.backend.domain.zzim.document.DongZzimDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface DongZzimRepository extends MongoRepository<DongZzimDoc, Integer> {

    long deleteByUserIdAndDongId(int userId, int dongId);

    List<DongZzimDoc> findAllByUserId(int userId); 
}
