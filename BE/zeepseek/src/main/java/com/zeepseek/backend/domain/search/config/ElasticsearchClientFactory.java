package com.zeepseek.backend.domain.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchClientFactory {

    // application.properties에서 elasticsearch.host 값을 읽어옴.
    @Value("${elasticsearch.host}")
    private String esHost;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // esHost 값에 따라 호스트가 동적으로 설정됨.
        RestClient restClient = RestClient.builder(
                new HttpHost(esHost, 9200)
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );
        return new ElasticsearchClient(transport);
    }
}