package com.dhruv.atlas.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.dhruv.atlas.dto.SearchApiResponse;
import com.dhruv.atlas.dto.SearchResult;
import com.dhruv.atlas.entity.DevDocs;
import com.dhruv.atlas.exception.SearchServiceException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final ElasticsearchClient esClient;

    public SearchService(ElasticsearchClient client) {
        this.esClient = client;
    }

    public SearchApiResponse search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank. ");
        }

        List<SearchResult> response = new ArrayList<>();
        try {
            SearchResponse<DevDocs> searchResponse = esClient.search(s -> s
                            .index("dev_docs")
                            .size(10)
                            .query(q -> q.bool(b -> b
                                    .should(sh -> sh.multiMatch(m -> m
                                            .query(query)
                                            .fields("title^3", "content")
                                    ))
                                    .should(sh -> sh.matchPhrase(mp -> mp
                                            .field("content")
                                            .query(query)
                                            .boost(2.0f)
                                    ))
                                    .minimumShouldMatch("1")
                            )),
                    DevDocs.class
            );

            for (var hit : searchResponse.hits().hits()) {
                DevDocs doc = hit.source();
                if (doc == null) {
                    continue;
                }

                response.add(SearchResult.builder()
                        .id(doc.getId())
                        .title(doc.getTitle())
                        .content(doc.getContent())
                        .source(doc.getSource())
                        .score(hit.score())
                        .build());
            }
            return new SearchApiResponse(query, response);
        } catch (ElasticsearchException | IOException e) {
            throw new SearchServiceException("Unable to query Elasticsearch. ", e);
        }
    }
}
