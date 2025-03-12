package com.ssafy.oauth.domain.repository;

import com.ssafy.oauth.domain.entity.Property;
import com.ssafy.oauth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    // 판매자 ID로 매물 목록 조회
    List<Property> findBySeller(User seller);

    // 특정 동에 있는 매물 목록 조회
    List<Property> findByDongDongId(Long dongId);

    // 계약 유형별 매물 목록 조회 (전세, 월세, 매매 등)
    List<Property> findByContractType(String contractType);

    // 방 유형별 매물 목록 조회 (원룸, 투룸, 빌라, 아파트 등)
    List<Property> findByRoomType(String roomType);

    // 가격 범위로 매물 목록 조회
    List<Property> findByPriceBetween(Long minPrice, Long maxPrice);
}