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
@Document(collection = "dong_zzim")
@CompoundIndex(name = "user_dong_unique_idx", def = "{'userId': 1, 'dongId': 1}", unique = true)
public class DongZzimDoc {

    int userId;

    int dongId;
}
