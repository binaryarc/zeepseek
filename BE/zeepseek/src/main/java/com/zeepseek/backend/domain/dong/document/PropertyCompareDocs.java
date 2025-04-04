package com.zeepseek.backend.domain.dong.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "property_compare_info")
public class PropertyCompareDocs {

    @Id
    private Integer compareId;

    private String compareSummary;
}
