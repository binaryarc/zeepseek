package com.zeepseek.backend.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "user_preference")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {
    
    @Id
    private Integer userId;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float safe;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float leisure;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float restaurant;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float health;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float convenience;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float transport;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float cafe;

    //전체 주소 텍스트
    @Column(length = 255)
    private String destination;

    // 세분화된 주소 정보 ( 추후 액티비티 로그 분석 시 용이할 수도 있어서 추가해둠)
    @Column(length = 50)
    private String sido; // 시/도 (예: 서울특별시)

    @Column(length = 50)
    private String sigungu; // 시/군/구 (예: 강남구)

    @Column(length = 100)
    private String roadName; // 도로명 (예: 테헤란로)

    @Column(length = 50)
    private String buildingInfo; // 건물 번호 및 정보 (예: 152)

    @Column(length = 50)
    private String detailAddress; // 상세 주소 (예: 10층 1001호)

    @Column(length = 10)
    private String zipCode; // 우편번호

    // 지리적 위치 정보
    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double latitude; // 위도

    @Column(columnDefinition = "DECIMAL(11,7)")
    private Double longitude; // 경도
    
    // 동 ID 정보
    @Column(name = "dong_id")
    private Integer dongId; // 동 ID (법정동 코드 뒤 2자리를 제외한 앞 8자리)
}