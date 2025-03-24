package com.zeepseek.backend.domain.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

public class ElasticsearchClientFactory {

    public static ElasticsearchClient createClient() {
        // 1. 저수준 RestClient 생성 (호스트, 포트 등 환경에 맞게 조정)
        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 9200)
        ).build();

        // 2. Transport 생성 (Jackson 기반의 매퍼 사용)
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        // 3. ElasticsearchClient 생성
        ElasticsearchClient client = new ElasticsearchClient(transport);
        return client;
    }
}