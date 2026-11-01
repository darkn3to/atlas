package com.dhruv.atlas.ingestion;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.dhruv.atlas.ingestion.embeddings.EmbeddingService;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import com.dhruv.atlas.ingestion.parser.DocumentationHtmlParser;
import com.dhruv.atlas.ingestion.parser.HtmlContentExtractor;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.dhruv.atlas.utils.Utils.fetchDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ChunkerTest {
    private final DocumentationHtmlParser parser =
            new DocumentationHtmlParser();

    @Autowired
    SearchIndexService searchIndexService;

    @Autowired
    HtmlContentExtractor htmlContentExtractor;

    @Autowired
    EmbeddingService embeddingService;

    //@Test
    public void test2() throws Exception {

        List<Float> embedding =
                embeddingService.generateEmbeddings(
                        "How does Redis persist data to disk?"
                );

        System.out.println("Dimensions: " + embedding.size());
        System.out.println("First five values: "
                + embedding.subList(0, 5));
    }

    @Test
    void test() throws Exception {
        //var extractor = new HtmlContentExtractor("https://readability-js-cwg4g4gcg9c2amb4.centralindia-01.azurewebsites.net");

        try (InputStream input = ChunkerTest.class.getResourceAsStream("/ingest.xml")) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList nodeList = doc.getElementsByTagName("loc");


            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                String url = node.getTextContent();

                Document document = fetchDocument(url);
                var result = htmlContentExtractor.extract(url, document.html());

                DocumentationPage page = parser.parse(url, result.html(), document.html());
                Chunker chunker = new Chunker(2000, new EmbeddingService());
                List<SearchChunk> s = chunker.performChunking(page);

                //BulkResponse response = searchIndexService.indexChunks(s);
                System.out.println((i+1));
            }
        }

       /* String url = "https://redis.io/docs/latest/develop/get-started/search-tutorial/vector-search/";

        Document document = fetchDocument(url);
        var result = htmlContentExtractor.extract(url, document.html());

        DocumentationPage page = parser.parse(url, result.html(), document.html());
        Chunker chunker = new Chunker(2000, new EmbeddingService());*/

        //DocumentationPage documentationPage = new DocumentationPage("Test1", "Test2", List.of(new ContentBlock(ContentType.PARAGRAPH, "My name is Dhruv.", (short) 0, List.of("Demo", "Bitch"), null, List.of()), new ContentBlock(ContentType.PARAGRAPH, "I am empty.", (short) 0, List.of(), null, List.of()), new ContentBlock(ContentType.PARAGRAPH, "Embeddings are sentences.", (short) 0, List.of("Vector search"), null, List.of()), new ContentBlock(ContentType.PARAGRAPH, "Vector search finds similar content. This sentence consists of gibberish and is purely there for testing purposes.", (short) 0, List.of("Vector search"), null, List.of())));

        //assertTrue(chunker.performChunking(documentationPage).isEmpty());

        /*List<SearchChunk> s = chunker.performChunking(page);
        BulkResponse response = searchIndexService.indexChunks(s);*/

        /*assertEquals(3, s.size());
        assertEquals("Embeddings are sentences.", chunker.performChunking(documentationPage).getFirst().textContent());
        assertEquals("Demo", s.getFirst().sectionPath().getFirst());
        assertEquals("Bitch", s.getFirst().sectionPath().getLast());
        assertEquals("Vector search", s.getLast().sectionPath().getFirst());*/
    }
}
