package com.dhruv.atlas;

import co.elastic.clients.elasticsearch.nodes.Ingest;
import com.dhruv.atlas.entity.DiscoveredPage;
import com.dhruv.atlas.entity.IngestionSource;
import com.dhruv.atlas.ingestion.Chunker;
import com.dhruv.atlas.ingestion.SearchIndexService;
import com.dhruv.atlas.ingestion.embeddings.EmbeddingService;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import com.dhruv.atlas.ingestion.pagediscovery.SitemapDiscoveryStrategy;
import com.dhruv.atlas.ingestion.parser.DocumentationHtmlParserService;
import com.dhruv.atlas.ingestion.parser.HtmlContentExtractorService;
import com.dhruv.atlas.repository.DocumentationSectionRepository;
import com.dhruv.atlas.service.PersistenceService;
import com.dhruv.atlas.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dhruv.atlas.utils.Utils.fetchDocument;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBootTest
public class DatabaseTest {
    @Autowired
    private PersistenceService persistenceService;

    @Autowired
    private SearchIndexService searchIndexService;

    @Autowired
    private DocumentationSectionRepository documentationSectionRepository;

    @Autowired
    private HtmlContentExtractorService htmlContentExtractorService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DocumentationHtmlParserService documentationHtmlParserService;

    @Autowired
    private SitemapDiscoveryStrategy sitemapDiscoveryStrategy;

    @Test
    void test() {
        AtomicInteger counter = new AtomicInteger(0);
        Chunker chunker = new Chunker(2000, embeddingService);

        String url = "http://localhost:8081/sample.html";
        IngestionSource src = new IngestionSource();
        src.setId(1);
        src.setName("Redis");
        src.setSourceType("Sitemap");
        src.setSourceUrl("https://redis.io/docs/latest/sitemap.xml");

        DiscoveredPage page = persistenceService.registerDiscoveredPage(
                    src,
                    url,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );

            Document document = fetchDocument(url);

            var result = htmlContentExtractorService.extract(
                    url,
                    document.html()
            );

            if (result.html() == null || result.html().isBlank()) {
                log.warn("No content extracted from page: {}. ", url);
                return;
            }

            String contentHash = Utils.generateHash(result.html());

            if (contentHash.equals(page.getContentHash())) {
                log.debug("Skipping unchanged page: {}. ", url);
                return;
            }

            log.debug("Processing page: {}. ", url);

            DocumentationPage documentationPage =
                    documentationHtmlParserService.parse(
                            url,
                            result.html(),
                            document.html()
                    );

            List<SearchChunk> chunks =
                    chunker.performChunking(documentationPage);

            /*searchIndexService.indexChunks(chunks);

             */

            persistenceService.markIndexed(
                    page,
                    contentHash
            );

            System.out.println("Processed pages: " + counter.incrementAndGet());

    }

    //@Test
    void testflow() {
        AtomicInteger counter = new AtomicInteger(0);
        Chunker chunker = new Chunker(2000, embeddingService);

        sitemapDiscoveryStrategy.discover(node -> {
            String url = node.url();

            DiscoveredPage page = persistenceService.registerDiscoveredPage(
                    node.source(),
                    url,
                    node.lastMod()
            );

            Document document = fetchDocument(url);

            var result = htmlContentExtractorService.extract(
                    url,
                    document.html()
            );

            if (result.html() == null || result.html().isBlank()) {
                log.warn("No content extracted from page: {}. ", url);
                return;
            }

            String contentHash = Utils.generateHash(result.html());

            if (contentHash.equals(page.getContentHash())) {
                log.debug("Skipping unchanged page: {}. ", url);
                return;
            }

            log.debug("Processing page: {}. ", url);

            DocumentationPage documentationPage =
                    documentationHtmlParserService.parse(
                            url,
                            result.html(),
                            document.html()
                    );

            List<SearchChunk> chunks =
                    chunker.performChunking(documentationPage);

            /*searchIndexService.indexChunks(chunks);

            persistenceService.markIndexed(
                    page,
                    contentHash
            );*/

            System.out.println("Processed pages: " + counter.incrementAndGet());
        });

        /*IngestionSource src = persistenceService.registerIngestionSource("Redis", "Sitemap", "https://redis.io/docs/latest/sitemap.xml");

        assert(src != null);
        System.out.println(src.getId());
        System.out.println(src.getName());*/
    }
}
