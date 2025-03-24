package com.zeepseek.backend.domain.property.repository;

import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("SELECT p.dongId AS dongId, COUNT(p) AS propertyCount " +
            "FROM Property p GROUP BY p.dongId")
    List<DongPropertyCountDto> countPropertiesByDong();

    @Query("SELECT p.guName AS guName, COUNT(p) AS propertyCount " +
            "FROM Property p GROUP BY p.guName")
    List<GuPropertyCountDto> countPropertiesByGu();

    List<Property> findByDongId(Integer dongId);

    List<Property> findByGuName(String guName);

}
