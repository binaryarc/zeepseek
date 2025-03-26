package com.zeepseek.backend.domain.dong.document;

import org.springframework.data.annotation.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "dong_compare_info")
public class DongCompareDocs {
    @Id
    private Integer compareId;

    private String compareSummary;
}
