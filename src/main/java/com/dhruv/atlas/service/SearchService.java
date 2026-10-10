package com.dhruv.atlas.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.dhruv.atlas.dto.SearchApiResponse;
import com.dhruv.atlas.dto.SearchResult;
import com.dhruv.atlas.entity.DevDocs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SearchService {

    private final ElasticsearchClient esClient;

    public SearchService(ElasticsearchClient client) {
        this.esClient = client;
    }

    public SearchApiResponse search(String query) {
        List<SearchResult> response=new ArrayList<>();

        if (esClient != null) {
            try {
                SearchResponse<DevDocs> a = esClient.search(s -> s
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

                for (var hit : a.hits().hits()) {
                    DevDocs doc = hit.source();
                    assert doc != null;

                    response.add(SearchResult.builder()
                            .id(doc.getId())
                            .title(doc.getTitle())
                            .content(doc.getContent())
                            .source(doc.getSource())
                            .score(hit.score())
                            .build());
                }
                return new SearchApiResponse(query, response);
            }
            catch (ElasticsearchException e) {
                log.error("Unable to process query. ");
            }
            catch (IOException e) {
                log.error("Internal Elastic Search Instance error. ");
            }
        }

        return new SearchApiResponse(query, new ArrayList<>());
    }
}
