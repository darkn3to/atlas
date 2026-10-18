/*
package com.dhruv.atlas.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest
public class DocumentFetchServiceTest {
    @Autowired
    DocumentFetchService documentFetchService;

    @Test
    void fetchDocumentation() {
        String url = "https://codefish.netlify.app";
        try {
            Document document = documentFetchService.fetch(url);
            assertNotNull(document);
            assertFalse(document.title().isBlank());
            System.out.println("Title: " + document.title());
            System.out.println("HTML title: " + document.select("h1").text());
        } catch (IOException e) {
            log.info("Unable to fetch required document from: {}", url);
        }

    }
}
*/
