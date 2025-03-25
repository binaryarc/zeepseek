package com.zeepseek.backend.domain.compare.document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// MongoDB 도큐먼트 (Spring Data MongoDB)
@Getter
@Setter
@Document(collection = "dong_info")
public class DongInfoDocs {

    @Id
    private Integer dongId;

    private String name;
    private String guName;

    private Float safe;
    private Float leisure;
    private Float restaurant;
    private Float health;
    private Float mart;
    private Float convenience;
    private Float transport;

    private LocalDateTime updatedAt;

    // GPT API를 통한 요약 정보
    private String summary;

}
