package com.zeepseek.backend.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.zeepseek.backend.domain.search.entity.SearchProperty;
import com.zeepseek.backend.domain.search.repository.SearchPropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ElasticsearchDataMigrationService {

    private final SearchPropertyRepository propertyRepository;
    private final ElasticsearchClient elasticsearchClient;

    public String migrate() throws Exception {
        List<SearchProperty> properties = propertyRepository.findAll();

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (SearchProperty property : properties) {
            bulkBuilder.operations(op -> op.index(idx -> idx
                    .index("properties") // 인덱스 이름 (필요에 따라 변경)
                    .id(String.valueOf(property.getPropertyId()))
                    .document(property)
            ));
        }

        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkBuilder.build());
        if (bulkResponse.errors()) {
            // 필요에 따라 세부 에러 처리를 추가
            return "Bulk 인덱싱 중 에러가 발생했습니다.";
        } else {
            return "Bulk 인덱싱이 성공적으로 완료되었습니다.";
        }
    }
}
