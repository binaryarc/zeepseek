package com.zeepseek.backend.domain.property.repository;

import com.zeepseek.backend.domain.property.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("propertyRepositoryProperty")
public interface PropertyRepository extends JpaRepository<Property, Long> {
    // 필요시 커스텀 메서드 추가 가능
}
