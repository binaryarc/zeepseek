package com.zeepseek.backend.domain.search.repository;

import com.zeepseek.backend.domain.search.entity.SearchProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchPropertyRepository extends JpaRepository<SearchProperty, Integer> {

    SearchProperty findByPropertyId(Integer propertyId);

}