package com.zeepseek.backend.domain.search.dto.response;

import com.zeepseek.backend.domain.search.dto.SearchProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordResponse {

    int total;
    List<SearchProperty> properties;
}
