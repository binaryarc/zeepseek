package com.zeepseek.backend.domain.property.repository;

import com.zeepseek.backend.domain.property.model.PropertyScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyScoreRepository extends JpaRepository<PropertyScore, Integer> {

    public PropertyScore findByPropertyId(int propertyId);

}
