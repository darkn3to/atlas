package com.dhruv.atlas.ingestion;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class HtmlContentExtractorTest {

    //@Test
    void extractsMainContentFromHtml() throws IOException {
        String url = "https://redis.io/docs/latest/develop/get-started/search-tutorial/vector-search/#how-vector-search-works";

        var extractor = new HtmlContentExtractor("https://readability-js-cwg4g4gcg9c2amb4.centralindia-01.azurewebsites.net");
        DocumentFetchService documentFetchService = new DocumentFetchService();
        Document document = documentFetchService.fetch(url);
        var result = extractor.extract(url, document.html());

        assertNotNull(result.html());
        assertFalse(result.html().isBlank());
        //assertTrue(result.html().contains("server.port"));
        assertEquals(url, result.sourceUrl());
        System.out.println(result.html());
    }
}