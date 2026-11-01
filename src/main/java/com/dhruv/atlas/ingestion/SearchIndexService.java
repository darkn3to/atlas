package com.dhruv.atlas.ingestion;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.dhruv.atlas.exception.DocumentIngestionException;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class SearchIndexService {
    private final ElasticsearchClient esClient;

    public SearchIndexService(ElasticsearchClient elasticsearchClient) {
        this.esClient = elasticsearchClient;
    }

    public static String generateId(SearchChunk chunk) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (DataOutputStream out = new DataOutputStream(buffer)) {
                writeField(out, chunk.sourceUrl());
                writeField(out, String.join("", chunk.sectionPath()));
                writeField(out, chunk.textContent());
            }

            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(buffer.toByteArray());

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate chunk ID", e);
        }
    }

    private static void writeField(DataOutputStream out, String value)
            throws java.io.IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public BulkResponse indexChunks(List<SearchChunk> chunks) {
        BulkResponse bulkResponse;
        try {
            bulkResponse = esClient.bulk(bulk -> {
                for (SearchChunk searchChunk : chunks) {
                    bulk.operations(operation -> operation
                            .index(index -> index
                                    .index("developer_docs")
                                    .id(generateId(searchChunk))
                                    .document(searchChunk)));
                }

                return bulk;
            });

            if (bulkResponse.errors()) {
                String failures = bulkResponse.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.id() + ": " + item.error().reason())
                        .limit(10)
                        .reduce((left, right) -> left + "; " + right)
                        .orElse("No item-level error details were returned");
                throw new DocumentIngestionException(
                        "Elasticsearch rejected one or more documents in the bulk request: "
                                + failures + ". ");
            }

            return bulkResponse;
        } catch (IOException | ElasticsearchException e) {
            throw new DocumentIngestionException("Unable to send documents to Elasticsearch. ", e);
        }

    }
}
