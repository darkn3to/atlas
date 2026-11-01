package com.dhruv.atlas.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.dhruv.atlas.dto.SearchApiResponse;
import com.dhruv.atlas.dto.SearchResult;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import com.dhruv.atlas.exception.SearchServiceException;
import com.dhruv.atlas.ingestion.embeddings.EmbeddingService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {

    private final int RRF_K = 60;

    private final ElasticsearchClient esClient;

    private final EmbeddingService embeddingService;

    public SearchService(ElasticsearchClient client, EmbeddingService embeddingService) {
        this.esClient = client;
        this.embeddingService = embeddingService;
    }

    public SearchApiResponse search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank. ");
        }

        List<SearchResult> response = new ArrayList<>();
        try {
            SearchResponse<SearchChunk> searchResponse = esClient.search(s -> s
                            .index("developer_docs")
                            .size(10)
                            .query(q -> q.bool(b -> b
                                    .should(sh -> sh.matchPhrase(mp -> mp
                                            .field("title")
                                            .query(query)
                                            .boost(12.0f)
                                    ))
                                    .should(sh -> sh.matchPhrase(mp -> mp
                                            .field("sectionPath")
                                            .query(query)
                                            .boost(8.0f)
                                    ))
                                    .should(sh -> sh.multiMatch(m -> m
                                            .query(query)
                                            .fields("title^6", "sectionPath^4", "textContent")
                                            .operator(Operator.And)
                                            .boost(3.0f)
                                    ))
                                    /*.should(sh -> sh.multiMatch(m -> m
                                            .query(query)
                                            .fields("title^6", "sectionPath^4", "textContent")
                                    ))*/
                                    .minimumShouldMatch("1")
                            )),
                    SearchChunk.class
            );

            for (var hit : searchResponse.hits().hits()) {
                SearchChunk doc = hit.source();
                if (doc == null) {
                    continue;
                }

                response.add(SearchResult.builder()
                        .id(hit.id())
                        .title(doc.title())
                        .sectionPath(String.join(" / ", doc.sectionPath()))
                        .content(doc.textContent())
                        .url(doc.sourceUrl())
                        .score(hit.score())
                        .build());
            }
            return new SearchApiResponse(query, response);
        } catch (ElasticsearchException | IOException e) {
            throw new SearchServiceException("Unable to query Elasticsearch. ", e);
        }
    }

    public SearchApiResponse vectorSearch(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank. ");
        }

        List<Float> searchQueryEmbedding = embeddingService.generateEmbeddings(query);

        List<SearchResult> response = new ArrayList<>();
        try {
            SearchResponse<SearchChunk> searchResponse = esClient.search(s -> s
                            .index("developer_docs")
                            .knn(k -> k
                                    .field("embedding")
                                    .queryVector(searchQueryEmbedding)
                                    .k(10)
                                    .numCandidates(100)
                            ),
                    SearchChunk.class
            );

            for (var hit : searchResponse.hits().hits()) {
                SearchChunk doc = hit.source();
                if (doc == null) {
                    continue;
                }

                response.add(SearchResult.builder()
                        .id(hit.id())
                        .title(doc.title())
                        .sectionPath(String.join(" / ", doc.sectionPath()))
                        .content(doc.textContent())
                        .url(doc.sourceUrl())
                        .score(hit.score())
                        .build());
            }
            return new SearchApiResponse(query, response);
        } catch (ElasticsearchException | IOException e) {
            throw new SearchServiceException("Unable to query Elasticsearch. ", e);
        }
    }

    private void addRanks(
            @NonNull List<SearchResult> results,
            Map<String, Integer> ranks,
            Map<String, Double> rrfScores,
            Map<String, SearchResult> resultsById) {

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            String id = result.getId();
            int rank = i + 1;

            ranks.put(id, rank);

            double contribution = 1.0 / (RRF_K + rank);
            rrfScores.put(
                    id,
                    rrfScores.getOrDefault(id, 0.0) + contribution
            );

            resultsById.putIfAbsent(id, result);
        }
    }

    public SearchApiResponse hybridSearch(String query) {
        List<SearchResult> bm25Results = search(query).getResults();
        List<SearchResult> vectorResults = vectorSearch(query).getResults();

        HashMap<String, SearchResult> resultsById = new HashMap<>();

        Map<String, Integer> bm25Ranks = new HashMap<>();
        Map<String, Integer> vectorRanks = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        addRanks(bm25Results, bm25Ranks, rrfScores, resultsById);
        addRanks(vectorResults, vectorRanks, rrfScores, resultsById);

        List<SearchResult> hybridResults = new ArrayList<>();

        for (Map.Entry<String, Double> entry : rrfScores.entrySet()) {
            SearchResult result = resultsById.get(entry.getKey());
            double rrfScore = entry.getValue();
            /*System.out.printf(
                    "%s | BM25 rank: %d | Vector rank: %d | RRF: %.6f%n",
                    result.getId(),
                    bm25Ranks.get(result.getId()),
                    vectorRanks.get(result.getId()),
                    rrfScore
            );*/
            result.setScore(rrfScore);
            hybridResults.add(result);
        }

        hybridResults.sort(
                (a, b) -> Double.compare(b.getScore(), a.getScore())
        );

        return new SearchApiResponse(query, hybridResults);
    }
}
