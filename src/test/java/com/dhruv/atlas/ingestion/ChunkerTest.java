package com.dhruv.atlas.ingestion;

import com.dhruv.atlas.ingestion.model.ContentBlock;
import com.dhruv.atlas.ingestion.model.ContentType;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import com.dhruv.atlas.ingestion.parser.DocumentationHtmlParser;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChunkerTest {
    private final DocumentationHtmlParser parser =
            new DocumentationHtmlParser();
    @Test
    void test() throws Exception {
        String url = "https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html";

        var extractor = new HtmlContentExtractor("https://readability-js-cwg4g4gcg9c2amb4.centralindia-01.azurewebsites.net");
        DocumentFetchService documentFetchService = new DocumentFetchService();
        Document document = documentFetchService.fetch(url);
        var result = extractor.extract(url, document.html());

        DocumentationPage page = parser.parse(url, result.html(), document.html());
        Chunker chunker = new Chunker(3);
        DocumentationPage documentationPage =new DocumentationPage("Test1", "Test2", List.of(new ContentBlock(ContentType.PARAGRAPH, "My name is Dhruv.", (short) 0, List.of("Demo", "Bitch"), null, List.of()), new ContentBlock(ContentType.PARAGRAPH, "I am empty.", (short) 0, List.of(), null, List.of()), new ContentBlock(ContentType.PARAGRAPH, "Embeddings are sentences.", (short) 0, List.of("Vector search"), null, List.of()), new ContentBlock(ContentType.PARAGRAPH, "Vector search finds similar content.", (short) 0, List.of("Vector search"), null, List.of())));

        //assertTrue(chunker.performChunking(documentationPage).isEmpty());
        List<SearchChunk> s = chunker.performChunking(page);
        for (int i = 0; i<s.size(); i++) {
            System.out.println("Chunk "+ i + " size: " +s.get(i).textContent().length());
        }
        /*assertEquals(3, s.size());
        assertEquals("Embeddings are sentences.", chunker.performChunking(documentationPage).getFirst().textContent());
        assertEquals("Demo", s.getFirst().sectionPath().getFirst());
        assertEquals("Bitch", s.getFirst().sectionPath().getLast());
        assertEquals("Vector search", s.getLast().sectionPath().getFirst());*/
    }
}
