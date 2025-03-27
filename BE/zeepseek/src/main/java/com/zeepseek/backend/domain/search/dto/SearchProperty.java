package com.zeepseek.backend.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchProperty {
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
    private Float latitude;
    private Float longitude;
    private Integer dongId;
    private String guName;
    private String dongName;
    private String computedRoomType;

    @Override
    public String toString() {
        return "PropertyDTO{" +
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
                ", computedRoomType='" + computedRoomType + '\'' +
                '}';
    }
}
