package com.zeepseek.backend.domain.property.repository;

import com.zeepseek.backend.domain.property.model.PropertyScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyScoreRepository extends JpaRepository<PropertyScore, Long> {
    Optional<PropertyScore> findByPropertyId(Integer propertyId);
}
