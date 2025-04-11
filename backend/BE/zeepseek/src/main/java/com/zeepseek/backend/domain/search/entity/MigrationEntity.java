package com.zeepseek.backend.domain.search.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "property")
public class MigrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Integer propertyId;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "room_type")
    private String roomType;

    @Column(name = "contract_type")
    private String contractType;

    @Column(name = "price")
    private String price;

    @Column(name = "address")
    private String address;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "area")
    private String area;

    @Column(name = "floor_info")
    private String floorInfo;

    @Column(name = "room_bath_count")
    private String roomBathCount;

    @Column(name = "maintenance_fee")
    private Integer maintenanceFee;

    @Column(name = "move_in_date")
    private String moveInDate;

    @Column(name = "direction")
    private String direction;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "deposit")
    private Integer deposit;

    @Column(name = "monthly_rent")
    private Integer monthlyRent;

    @Column(name = "latitude")
    private Float latitude;

    @Column(name = "longitude")
    private Float longitude;

    @Column(name = "dong_id")
    private Integer dongId;

    @Column(name = "gu_name")
    private String guName;

    @Column(name = "dong_name")
    private String dongName;

    @Override
    public String toString() {
        return "SearchProperty{" +
                "propertyId=" + propertyId +
                ", sellerId=" + sellerId +
                ", roomType='" + roomType + '\'' +
                ", contractType='" + contractType + '\'' +
                ", price='" + price + '\'' +
                ", address='" + address + '\'' +
                ", description='" + description + '\'' +
                ", area='" + area + '\'' +
                ", floorInfo='" + floorInfo + '\'' +
                ", roomBathCount='" + roomBathCount + '\'' +
                ", maintenanceFee=" + maintenanceFee +
                ", moveInDate='" + moveInDate + '\'' +
                ", direction='" + direction + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", salePrice=" + salePrice +
                ", deposit=" + deposit +
                ", monthlyRent=" + monthlyRent +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", dongId=" + dongId +
                ", guName='" + guName + '\'' +
                ", dongName='" + dongName + '\'' +
                '}';
    }
}