package com.dhruv.atlas.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.transport.TransportUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class ElasticSearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String serverUrl = "https://localhost:9200";
        ClassPathResource resource = new ClassPathResource("http_ca.crt");

        File certFile = copyCertificateToFile(resource);

        final SSLContext sslContext;
        try {
            sslContext = TransportUtils
                    .sslContextFromHttpCaCrt(certFile);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create SSL context for Elasticsearch. ", e);
        }

        try {
            ElasticsearchClient esClient = ElasticsearchClient.of(b -> b
                    .host(serverUrl)
                    .usernameAndPassword("elastic", "elastic")
                    .sslContext(sslContext)
            );
            esClient.info();
            return esClient;
        } catch (IOException | ElasticsearchException e) {
            throw new IllegalStateException(
                    "Unable to establish connection with Elasticsearch. " +
                            "Ensure it is running and the connection configuration is correct.", e);
        }

    }

    private File copyCertificateToFile(ClassPathResource resource) {
        try {
            Path certificate = Files.createTempFile("atlas-elasticsearch-ca-", ".crt");
            try (var inputStream = resource.getInputStream()) {
                Files.copy(inputStream, certificate, StandardCopyOption.REPLACE_EXISTING);
            }
            certificate.toFile().deleteOnExit();
            return certificate.toFile();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to load Elasticsearch CA certificate from the classpath. ", e);
        }
    }
}
