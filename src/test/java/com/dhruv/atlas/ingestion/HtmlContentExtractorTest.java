package com.dhruv.atlas.ingestion;

import com.dhruv.atlas.ingestion.parser.HtmlContentExtractorService;
import org.jsoup.nodes.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static com.dhruv.atlas.utils.Utils.fetchDocument;

class HtmlContentExtractorTest {
   // @Test
    void extractsMainContentFromHtml() throws Exception {
        //String url = "https://redis.io/docs/latest/develop/get-started/search-tutorial/vector-search/#how-vector-search-works";

        var extractor = new HtmlContentExtractorService("https://readability-js-cwg4g4gcg9c2amb4.centralindia-01.azurewebsites.net");
        /*Document document = fetchDocument(url);
        var result = extractor.extract(url, document.html());*/

        try (InputStream input = ChunkerTest.class.getResourceAsStream("/ingest.xml")) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList nodeList = doc.getElementsByTagName("loc");


            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                String url = node.getTextContent();

                Document document = fetchDocument(url);
                var result = extractor.extract(url, document.html());
                System.out.println(i);
            }
        }

        /*assertNotNull(result.html());
        assertFalse(result.html().isBlank());
        //assertTrue(result.html().contains("server.port"));
        assertEquals(url, result.sourceUrl());
        System.out.println(result.html());*/
    }
}