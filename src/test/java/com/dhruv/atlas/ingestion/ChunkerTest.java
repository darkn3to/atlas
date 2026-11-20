package com.dhruv.atlas.ingestion;

import com.dhruv.atlas.ingestion.embeddings.EmbeddingService;
import com.dhruv.atlas.ingestion.pagediscovery.SitemapDiscoveryStrategy;
import com.dhruv.atlas.ingestion.parser.DocumentationHtmlParserService;
import com.dhruv.atlas.ingestion.parser.HtmlContentExtractorService;
import com.dhruv.atlas.repository.DocumentationSectionRepository;
import com.dhruv.atlas.service.PersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ChunkerTest {
    @Autowired
    SearchIndexService searchIndexService;

    @Autowired
    DocumentationSectionRepository documentationSectionRepository;

    @Autowired
    HtmlContentExtractorService htmlContentExtractorService;

    @Autowired
    EmbeddingService embeddingService;

    @Autowired
    DocumentationHtmlParserService documentationHtmlParserService;

    @Autowired
    SitemapDiscoveryStrategy sitemapDiscoveryStrategy;

    @Autowired
    PersistenceService persistenceService;

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

    //@Test
    void test() throws Exception {
        //var extractor = new HtmlContentExtractorService("https://readability-js-cwg4g4gcg9c2amb4.centralindia-01.azurewebsites.net");

        /*try (InputStream input = ChunkerTest.class.getResourceAsStream("/ingest.xml")) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList nodeList = doc.getElementsByTagName("loc");

            Chunker chunker = new Chunker(2000, new EmbeddingService());
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                String url = node.getTextContent();

                Document document = fetchDocument(url);
                var result = htmlContentExtractorService.extract(url, document.html());

                DocumentationPage page = documentationHtmlParserService.parse(url, result.html(), document.html());
                List<SearchChunk> s = chunker.performChunking(page);

                searchIndexService.indexChunks(s);
                System.out.println((i+1));
            }
        }*/

        /*List<DiscoveredPage> nodeLists = sitemapDiscoveryStrategy.discover();

        for (int i = 0; i < nodeLists.size(); i++) {
            DiscoveredPage node = nodeLists.get(i);

            String url = node.url();

            Document document = fetchDocument(url);
            var result = htmlContentExtractorService.extract(url, document.html());

            DocumentationPage page = documentationHtmlParserService.parse(url, result.html(), document.html());
            Chunker chunker = new Chunker(2000, new EmbeddingService(), documentationSectionRepository);
            List<SearchChunk> s = chunker.performChunking(page);

            //searchIndexService.indexChunks(s);
            System.out.println((i+1));
        }*/

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
    /*@Test
    void testIncrementalIngestion() throws Exception {

        // 1. Retrieve registered source
        IngestionSource source = entryService.ingestionSourceRepository.findById(1)
                .orElseThrow(() ->
                        new IllegalStateException("Source not found"));

        // 2. Discover pages from sitemap
        List<SitemapPage> sitemapPages =
                sitemapDiscoveryStrategy.discover(source.getSourceUrl());

        System.out.println("Discovered pages: " + sitemapPages.size());

        // 3. Process discovered pages
        for (SitemapPage sitemapPage : sitemapPages) {

            String url = sitemapPage.url();

            // 3.1 Find existing page or create a new one
            DiscoveredPage page = entryService.discoveredPageRepository
                    .findBySourceIdAndUrl(source.getId(), url)
                    .orElseGet(() -> {
                        DiscoveredPage newPage = new DiscoveredPage();
                        newPage.setSource(source);
                        newPage.setUrl(url);
                        return newPage;
                    });

            // 3.2 Update sitemap metadata
            page.setLastModified(sitemapPage.lastModified());
            page.setLastSeenAt(LocalDateTime.now());

            page = discoveredPageRepository.save(page);

            // 4. Fetch documentation HTML
            Document document = fetchDocument(url);

            // 5. Extract meaningful HTML
            var result = htmlContentExtractorService.extract(
                    url,
                    document.html()
            );

            String cleanedHtml = result.html();

            // 6. Generate page hash
            String pageHash = HashUtils.generateHash(cleanedHtml);

            // 7. Skip unchanged pages
            if (pageHash.equals(page.getContentHash())) {
                System.out.println("Unchanged: " + url);
                continue;
            }

            System.out.println("Changed/New: " + url);

            // 8. Parse documentation
            DocumentationPage documentationPage =
                    documentationHtmlParserService.parse(
                            url,
                            cleanedHtml,
                            document.html()
                    );

            // 9. Generate chunks
            Chunker chunker = new Chunker(
                    2000,
                    embeddingService,
                    documentationSectionRepository
            );

            List<SearchChunk> chunks =
                    chunker.performChunking(documentationPage);

            // 10. Index chunks
            searchIndexService.indexChunks(chunks);

            // 11. Update page metadata after successful indexing
            page.setContentHash(pageHash);
            page.setLastIndexedAt(LocalDateTime.now());

            discoveredPageRepository.save(page);

            System.out.println("Indexed chunks: " + chunks.size());
        }
    }*/
}
