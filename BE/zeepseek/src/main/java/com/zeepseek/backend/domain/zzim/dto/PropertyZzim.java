package com.zeepseek.backend.domain.zzim.dto;

import jakarta.persistence.*;

import java.time.LocalDateTime;

public class PropertyZzim {

    private Integer propertyId;
    private Integer sellerId;
    private String roomType;
    private String contractType;
    private String price;
    private String address;
    private String description;
    private String area;
    private String floorInfo;
    private String roomBathCount;
    private Integer maintenanceFee;
    private String moveInDate;
    private String direction;
    private String imageUrl;
    private Integer salePrice;
    private Integer deposit;
    private Integer monthlyRent;
    private Double latitude;
    private Double longitude;
    private Integer dongId;
    private String guName;
    private LocalDateTime createdAt;
}
