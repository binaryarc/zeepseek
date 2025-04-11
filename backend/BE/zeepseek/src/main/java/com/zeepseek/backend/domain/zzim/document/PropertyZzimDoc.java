package com.zeepseek.backend.domain.zzim.document;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@Document(collection = "property_zzim")
@CompoundIndex(name = "user_property_unique_idx", def = "{'userId': 1, 'propertyId': 1}", unique = true)
public class PropertyZzimDoc {

    int userId;

    int propertyId;

    @Override
    public String toString() {
        return "PropertyZzimDoc{" +
                "userId=" + userId +
                ", propertyId=" + propertyId +
                '}';
    }
}
