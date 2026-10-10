package com.dhruv.atlas.ingestion;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.dhruv.atlas.entity.DevDocs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
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

    public void ingestDocuments() {
        List<DevDocs> documents = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new ClassPathResource("bulk.json").getInputStream()))) {
            String actionLine;

            while ((actionLine = reader.readLine())!=null) {
                String documentLine = reader.readLine();

                if (documentLine == null) {
                    log.error("Document line not found. ");
                }
                try {
                    DevDocs document = objectMapper.readValue(documentLine, DevDocs.class);
                    documents.add(document);
                }
                catch (JsonProcessingException e ) {
                    log.error("Unable to parse JSON document in bulk.json. ");
                }
            }

            if (documents.isEmpty()) {
                return;
            }

            BulkResponse response = esClient.bulk(bulk -> {
                for (DevDocs document : documents) {
                    bulk.operations(operation -> operation
                            .index(index -> index
                                    .index("dev_docs_2")
                                    .id(document.getId())
                                    .document(document)));
                }
                return bulk;
            });
            if (response.errors()) {
                throw new IllegalStateException(
                        "Some documents failed to index");
            }
            log.info("Successfully indexed {} documents.", documents.size());
        }
        catch (IOException e) {
            log.error("Unable to locate bulk.json. ");
        }
    }
}
