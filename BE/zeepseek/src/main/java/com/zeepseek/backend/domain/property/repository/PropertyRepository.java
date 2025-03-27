package com.zeepseek.backend.domain.property.repository;

import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.dto.response.DongPropertyCountDto;
import com.zeepseek.backend.domain.property.dto.response.GuPropertyCountDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT p FROM Property p WHERE (p.roomBathCount LIKE '1/%' OR p.roomBathCount LIKE '2/%') AND p.dongId = :dongId")
    List<Property> findOneRoomByDongId(@Param("dongId") Integer dongId);

    @Query("SELECT p FROM Property p WHERE (p.roomType = '%빌라%' OR p.roomType LIKE '%주택%') AND p.dongId = :dongId")
    List<Property> findHouseByDongId(@Param("dongId") Integer dongId);

    @Query("SELECT p FROM Property p WHERE p.roomType = '%오피스텔%' AND p.dongId = :dongId")
    List<Property> findOfficeByDongId(@Param("dongId") Integer dongId);

    @Query("SELECT p FROM Property p WHERE (p.roomBathCount LIKE '1/%' OR p.roomBathCount LIKE '2/%') AND p.guName = :guName")
    List<Property> findOneRoomByGuName(@Param("guName") String guName);

    @Query("SELECT p FROM Property p WHERE (p.roomType = '빌라' OR p.roomType LIKE '%주택') AND p.guName = :guName")
    List<Property> findHouseByGuName(@Param("guName") String guName);

    @Query("SELECT p FROM Property p WHERE p.roomType = '오피스텔' AND p.guName = :guName")
    List<Property> findOfficeByGuName(@Param("guName") String guName);

    @Query("SELECT p FROM Property p WHERE p.roomBathCount LIKE '1/%' OR p.roomBathCount LIKE '2/%'")
    List<Property> findOneRoomProperties();

    @Query("SELECT p FROM Property p WHERE p.roomType = '빌라' OR p.roomType LIKE '%주택'")
    List<Property> findHouseProperties();

    @Query("SELECT p FROM Property p WHERE p.roomType = '오피스텔'")
    List<Property> findOfficeProperties();

    // 동별 원룸 매물 개수
    @Query("SELECT p.dongId AS dongId, COUNT(p) AS propertyCount " +
            "FROM Property p WHERE p.roomBathCount LIKE '1/%' OR p.roomBathCount LIKE '2/%' " +
            "GROUP BY p.dongId")
    List<DongPropertyCountDto> countOneRoomPropertiesByDong();

    // 동별 빌라/주택 매물 개수
    @Query("SELECT p.dongId AS dongId, COUNT(p) AS propertyCount " +
            "FROM Property p WHERE p.roomType = '빌라' OR p.roomType LIKE '%주택%' " +
            "GROUP BY p.dongId")
    List<DongPropertyCountDto> countHousePropertiesByDong();

    // 동별 오피스텔 매물 개수
    @Query("SELECT p.dongId AS dongId, COUNT(p) AS propertyCount " +
            "FROM Property p WHERE p.roomType = '오피스텔' " +
            "GROUP BY p.dongId")
    List<DongPropertyCountDto> countOfficePropertiesByDong();

    @Query(value = "SELECT * FROM property WHERE ST_Within(location, " +
            "ST_GeomFromText(CONCAT('POLYGON((', :minLat, ' ', :minLng, ', ', " +
            "              :minLat, ' ', :maxLng, ', ', :maxLat, ' ', :maxLng, ', ', " +
            "              :maxLat, ' ', :minLng, ', ', :minLat, ' ', :minLng, '))'), 4326))",
            nativeQuery = true)
    List<Property> findPropertiesInCell(@Param("minLng") double minLng,
                                        @Param("minLat") double minLat,
                                        @Param("maxLng") double maxLng,
                                        @Param("maxLat") double maxLat);

    @Query(value = "SELECT * FROM property WHERE ST_Within(location, " +
            "ST_GeomFromText(CONCAT('POLYGON((', :minLat, ' ', :minLng, ', ', " +
            "              :minLat, ' ', :maxLng, ', ', :maxLat, ' ', :maxLng, ', ', " +
            "              :maxLat, ' ', :minLng, ', ', :minLat, ' ', :minLng, '))'), 4326)) " +
            "AND (room_bath_count LIKE '1/%' OR room_bath_count LIKE '2/%')",
            nativeQuery = true)
    List<Property> findOneRoomPropertiesInCell(@Param("minLng") double minLng,
                                               @Param("minLat") double minLat,
                                               @Param("maxLng") double maxLng,
                                               @Param("maxLat") double maxLat);


    @Query(value = "SELECT * FROM property WHERE ST_Within(location, " +
            "ST_GeomFromText(CONCAT('POLYGON((', :minLat, ' ', :minLng, ', ', " +
            "              :minLat, ' ', :maxLng, ', ', :maxLat, ' ', :maxLng, ', ', " +
            "              :maxLat, ' ', :minLng, ', ', :minLat, ' ', :minLng, '))'), 4326)) " +
            "AND room_type = '오피스텔'",
            nativeQuery = true)
    List<Property> findOfficePropertiesInCell(@Param("minLng") double minLng,
                                              @Param("minLat") double minLat,
                                              @Param("maxLng") double maxLng,
                                              @Param("maxLat") double maxLat);

    @Query(value = "SELECT * FROM property WHERE ST_Within(location, " +
            "ST_GeomFromText(CONCAT('POLYGON((', :minLat, ' ', :minLng, ', ', " +
            "              :minLat, ' ', :maxLng, ', ', :maxLat, ' ', :maxLng, ', ', " +
            "              :maxLat, ' ', :minLng, ', ', :minLat, ' ', :minLng, '))'), 4326)) " +
            "AND (room_type = '빌라' OR room_type LIKE '%주택%')",
            nativeQuery = true)
    List<Property> findHousePropertiesInCell(@Param("minLng") double minLng,
                                             @Param("minLat") double minLat,
                                             @Param("maxLng") double maxLng,
                                             @Param("maxLat") double maxLat);
}
