package com.zeepseek.backend.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.zeepseek.backend.domain.search.dto.PropertyDTO;
import com.zeepseek.backend.domain.search.entity.SearchProperty;
import com.zeepseek.backend.domain.search.repository.SearchPropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchDataMigrationService {

    private final SearchPropertyRepository propertyRepository;
    private final ElasticsearchClient elasticsearchClient;

    public String migrate() throws Exception {
        // 인덱스 존재 여부 확인 및 생성
        boolean indexExists = elasticsearchClient.indices()
                .exists(e -> e.index("properties"))
                .value();
        if (!indexExists) {
            log.info("Index 'properties' does not exist. Creating index.");
            elasticsearchClient.indices().create(c -> c.index("properties"));
        } else {
            log.info("Index 'properties' already exists.");
        }

        List<SearchProperty> properties = propertyRepository.findAll();
        int batchSize = 100; // 한 번에 처리할 문서 수 조정 (필요에 따라 변경)
        int total = properties.size();
        log.info("총 {}개의 문서를 인덱싱합니다.", total);

        for (int i = 0; i < total; i += batchSize) {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            int end = Math.min(i + batchSize, total);
            List<SearchProperty> batch = properties.subList(i, end);

            for (SearchProperty property : batch) {
                log.info("인덱싱 대상 문서: {}", property);
                // 엔티티를 DTO로 매핑
                PropertyDTO propertyDTO = PropertyDTO.builder()
                        .propertyId(property.getPropertyId())
                        .sellerId(property.getSellerId())
                        .roomType(property.getRoomType())
                        .contractType(property.getContractType())
                        .price(property.getPrice())
                        .address(property.getAddress())
                        .description(property.getDescription())
                        .area(property.getArea())
                        .floorInfo(property.getFloorInfo())
                        .roomBathCount(property.getRoomBathCount())
                        .maintenanceFee(property.getMaintenanceFee())
                        .moveInDate(property.getMoveInDate())
                        .direction(property.getDirection())
                        .imageUrl(property.getImageUrl())
                        .salePrice(property.getSalePrice())
                        .deposit(property.getDeposit())
                        .monthlyRent(property.getMonthlyRent())
                        .latitude(property.getLatitude())
                        .longitude(property.getLongitude())
                        .dongId(property.getDongId())
                        .guName(property.getGuName())
                        .title(property.getTitle())
                        .build();

                bulkBuilder.operations(op -> op.index(idx -> idx
                        .index("properties")
                        .id(String.valueOf(propertyDTO.getPropertyId()))
                        .document(propertyDTO)
                ));
            }

            BulkResponse bulkResponse = elasticsearchClient.bulk(bulkBuilder.build());
            if (bulkResponse.errors()) {
                StringBuilder errorMessages = new StringBuilder();
                bulkResponse.items().forEach(item -> {
                    if (item.error() != null) {
                        errorMessages.append(item.error()).append("; ");
                    }
                });
                log.error("배치 인덱싱 중 에러 발생 (문서 {} ~ {}): {}", i, end, errorMessages.toString());
                return "Bulk 인덱싱 중 에러가 발생했습니다: " + errorMessages.toString();
            } else {
                log.info("문서 {} ~ {} 배치 인덱싱이 성공적으로 완료되었습니다.", i, end);
            }
        }
        log.info("모든 문서의 인덱싱이 성공적으로 완료되었습니다.");
        return "Bulk 인덱싱이 성공적으로 완료되었습니다.";
    }
}
