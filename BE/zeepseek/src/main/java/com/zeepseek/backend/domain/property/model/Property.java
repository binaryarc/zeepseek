package com.zeepseek.backend.domain.property.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "property")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Integer propertyId;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "room_type", nullable = false, length = 20)
    private String roomType;

    @Column(name = "contract_type", nullable = false, length = 20)
    private String contractType;

    @Column(name = "price", nullable = false, length = 30)
    private String price;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "area", length = 50)
    private String area;

    @Column(name = "floor_info", length = 50)
    private String floorInfo;

    @Column(name = "room_bath_count", length = 50)
    private String roomBathCount;

    @Column(name = "maintenance_fee")
    private Integer maintenanceFee;

    @Column(name = "move_in_date", length = 50)
    private String moveInDate;

    @Column(name = "direction", length = 50)
    private String direction;

    @Lob
    @Column(name = "image_url", columnDefinition = "TEXT") // 필요시 columnDefinition 추가
    private String imageUrl;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "deposit")
    private Integer deposit;

    @Column(name = "monthly_rent")
    private Integer monthlyRent;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "dong_id", nullable = false)
    private Integer dongId;

    @Column(name = "gu_name", nullable = false, length = 10)
    private String guName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "title", nullable = false, length = 255)
    private String title;
}
