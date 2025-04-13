package com.zeepseek.backend.domain.search.document;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogDocument {
    private String action;
    private Integer age;
    private String computedRoomType;
    private String contractType;
    private String dongId;
    private Integer gender;
    private Integer propertyId;
    private String roomType;
    private Date time; // time 필드는 Elasticsearch의 날짜 타입과 매핑
    private String type;
    private Integer userId;
}
