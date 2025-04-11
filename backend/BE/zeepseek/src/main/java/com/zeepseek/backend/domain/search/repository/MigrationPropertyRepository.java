package com.zeepseek.backend.domain.search.repository;

import com.zeepseek.backend.domain.search.entity.MigrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrationPropertyRepository extends JpaRepository<MigrationEntity, Integer> {

    MigrationEntity findByPropertyId(Integer propertyId);

}