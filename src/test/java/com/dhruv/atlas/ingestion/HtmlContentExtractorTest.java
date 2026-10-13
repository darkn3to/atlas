package com.dhruv.atlas.ingestion;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class HtmlContentExtractorTest {

    @Test
    void extractsMainContentFromHtml() throws IOException {
        String url = "https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html#features.external-config.encrypting";

        var extractor = new HtmlContentExtractor("https://xyz.com");
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