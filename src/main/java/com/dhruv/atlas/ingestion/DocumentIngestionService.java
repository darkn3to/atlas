package com.dhruv.atlas.ingestion;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.dhruv.atlas.exception.DocumentIngestionException;
import com.dhruv.atlas.entity.DevDocs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DocumentIngestionService {
    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    public DocumentIngestionService(ElasticsearchClient client) {
        this.esClient = client;
        this.objectMapper = new ObjectMapper();
    }

    private void validateIndexAction(String actionLine, int lineNumber) throws JsonProcessingException {
        JsonNode action = objectMapper.readTree(actionLine);
        JsonNode index = (action == null) ? null : action.get("index");
        if (action == null || !action.isObject() || index == null || !index.isObject()) {
            throw new IllegalArgumentException(
                    "Expected an index action on line " + lineNumber + ". ");
        }
    }

    private void validateDocument(DevDocs document, int lineNumber) {
        if (document.getId() == null || document.getId().isBlank()) {
            throw new IllegalStateException(
                    "Missing document ID near line " + lineNumber + ". ");
        }
        if (document.getTitle() == null || document.getTitle().isBlank()) {
            throw new IllegalStateException(
                    "Missing document title for ID: " + document.getId() + ". ");
        }
        if (document.getContent() == null || document.getContent().isBlank()) {
            throw new IllegalStateException(
                    "Missing document content for ID: " + document.getId() + ". ");
        }
        if (document.getSource() == null || document.getSource().isBlank()) {
            throw new IllegalStateException(
                    "Missing document source for ID: " + document.getId() + ". ");
        }
    }

    public void ingestDocuments() {
        List<DevDocs> documents = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new ClassPathResource("bulk.json").getInputStream()))) {

            int lineNumber = 0;
            String actionLine;
            while ((actionLine = reader.readLine()) != null) {
                lineNumber++;

                String documentLine = reader.readLine();
                if (documentLine == null) {
                    throw new DocumentIngestionException(
                            "Missing document after action on line " + lineNumber + ". ");
                }
                lineNumber++;

                try {
                    validateIndexAction(actionLine, lineNumber - 1);
                    DevDocs document = objectMapper.readValue(documentLine, DevDocs.class);
                    validateDocument(document, lineNumber);
                    documents.add(document);
                } catch (JsonProcessingException | IllegalArgumentException e) {
                    throw new DocumentIngestionException(
                            "Unable to parse document in bulk.json on line " + lineNumber + ". ", e);
                }
                catch (IllegalStateException e) {
                    throw new DocumentIngestionException(
                            "Empty value in document on line: " + lineNumber + ", ", e
                    );
                }
            }

            if (documents.isEmpty()) {
                throw new DocumentIngestionException("bulk.json does not contain any documents. ");
            }

            BulkResponse response;
            try {
                response = esClient.bulk(bulk -> {
                    for (DevDocs document : documents) {
                        bulk.operations(operation -> operation
                                .index(index -> index
                                        .index("dev_docs_2")
                                        .id(document.getId())
                                        .document(document)));
                    }
                    return bulk;
                });
            } catch (IOException | ElasticsearchException e) {
                throw new DocumentIngestionException("Unable to send documents to Elasticsearch. ", e);
            }

            if (response.errors()) {
                String failures = response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.id() + ": " + item.error().reason())
                        .limit(10)
                        .reduce((left, right) -> left + "; " + right)
                        .orElse("No item-level error details were returned");
                throw new DocumentIngestionException(
                        "Elasticsearch rejected one or more documents in the bulk request: "
                                + failures + ". ");
            }

            log.info("Successfully indexed {} documents.", documents.size());
        } catch (IOException e) {
            throw new DocumentIngestionException("Unable to read bulk.json. ", e);
        }
    }
}
