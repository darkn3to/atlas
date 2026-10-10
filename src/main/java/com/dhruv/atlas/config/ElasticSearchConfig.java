package com.dhruv.atlas.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.transport.TransportUtils;
import com.dhruv.atlas.exception.ESConnectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class ElasticSearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String serverUrl = "https://localhost:9200";
        ClassPathResource resource = new ClassPathResource("http_ca.crt");

        File certFile;
        try {
            certFile = resource.getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final SSLContext sslContext;
        try {
            sslContext = TransportUtils
                    .sslContextFromHttpCaCrt(certFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ElasticsearchClient esClient = null;

        try {
            esClient = ElasticsearchClient.of(b -> b
                    .host(serverUrl)
                    .usernameAndPassword("elastic", "elastic")
                    .sslContext(sslContext)
            );
            esClient.info();
        } catch (Exception e) {
            log.error("Unable to establish connection with ES Client. Ensure Elasticsearch instance is running and check Elasticsearch connection configuration. ");
        }

        return esClient;
    }
}
