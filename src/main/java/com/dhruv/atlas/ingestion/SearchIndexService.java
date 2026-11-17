package com.dhruv.atlas.ingestion;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.dhruv.atlas.exception.DocumentIngestionException;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.dhruv.atlas.utils.Utils.generateChunkId;

@Service
public class SearchIndexService {
    private final ElasticsearchClient esClient;

    public SearchIndexService(ElasticsearchClient elasticsearchClient) {
        this.esClient = elasticsearchClient;
    }

    public BulkResponse indexChunks(List<SearchChunk> chunks) {
        BulkResponse bulkResponse;
        try {
            bulkResponse = esClient.bulk(bulk -> {
                for (SearchChunk searchChunk : chunks) {
                    bulk.operations(operation -> operation
                            .index(index -> index
                                    .index("developer_docs3")
                                    .id(generateChunkId(searchChunk))
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
