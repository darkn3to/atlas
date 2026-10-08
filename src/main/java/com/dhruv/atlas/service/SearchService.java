package com.dhruv.atlas.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.transport.TransportUtils;
import com.dhruv.atlas.dto.SearchApiResponse;
import com.dhruv.atlas.dto.SearchResult;
import com.dhruv.atlas.entity.DevDocs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SearchService {

    public SearchApiResponse search(String query) {
        String serverUrl = "https://localhost:9200";

        ClassPathResource resource = new ClassPathResource("http_ca.crt");

        List<SearchResult> response=new ArrayList<>();

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

        try (ElasticsearchClient esClient = ElasticsearchClient.of(b -> b
                .host(serverUrl)
                .usernameAndPassword("elastic", "elastic")
                .sslContext(sslContext)
        )) {
            SearchResponse<DevDocs> a = esClient.search(s -> s
                            .index("dev_docs")
                            .size(10)
                            .query(q -> q
                                    .multiMatch(m -> m
                                            .query(query)
                                            .fields("title^3", "content")
                                    )
                            ),
                    DevDocs.class
            );

            for (var hit : a.hits().hits()) {
                DevDocs doc = hit.source();
                assert doc != null;
                //response.add(doc);
                response.add(SearchResult.builder()
                        .id(doc.getId())
                        .title(doc.getTitle())
                        .content(doc.getContent())
                        .source(doc.getSource())
                        .score(hit.score())
                        .build());
            }
            return new SearchApiResponse(query, response);
        } catch (Exception e) {
            log.error("Unable to establish connection with ES Client. ");
            //e.printStackTrace();
        }
        return new SearchApiResponse(query, new ArrayList<>());
    }
}
