package com.dhruv.atlas.ingestion.embeddings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.dhruv.atlas.utils.Utils.hasCause;

@Slf4j
@Service
public class EmbeddingService {
    private static final String EMBEDDING_URL =
            "http://localhost:11434/api/embed";

    private static final String MODEL = "nomic-embed-text";

    private static final int EXPECTED_DIMENSIONS = 768;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final RestClient restClient;

    public EmbeddingService() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(EMBEDDING_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public record EmbeddingRequest(
            String model,
            String input
    ) {}

    public List<Float> generateEmbeddings(@NonNull String content) {
        if (content.isBlank()) {
            throw new IllegalArgumentException("No content for creating vector embedding. ");
        }

        try {
            String response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new EmbeddingRequest(MODEL, content))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode vector = root.path("embeddings").path(0);

            if (!vector.isArray()) {
                throw new IOException(
                        "Ollama response did not contain an embedding. "
                );
            }

            List<Float> embedding = new ArrayList<>(vector.size());

            for (JsonNode value : vector) {
                if (!value.isNumber()) {
                    throw new IOException(
                            "Embedding contains a non-numeric value. "
                    );
                }

                float number = value.floatValue();

                if (!Float.isFinite(number)) {
                    throw new IOException(
                            "Embedding contains a non-finite value. "
                    );
                }

                embedding.add(number);
            }

            if (embedding.size() != EXPECTED_DIMENSIONS) {
                throw new IOException(
                        "Expected " + EXPECTED_DIMENSIONS
                                + " dimensions but received "
                                + embedding.size() + ". "
                );
            }

            return List.copyOf(embedding);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Embedding service returned HTTP "
                    + e.getStatusCode().value() + ". ", e
            );
        } catch (RestClientException e) {
            if (hasCause(e, SocketTimeoutException.class)) {
                throw new IllegalStateException(
                        "Timed out while communicating with Embedding service. ", e
                );
            }

            if (hasCause(e, ConnectException.class)) {
                throw new IllegalStateException(
                        "Could not connect to Embedding service. Ensure the service is running and reachable. ",
                        e
                );
            }

            throw new IllegalStateException(
                    "Communication with Embedding service failed. ",
                    e
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
