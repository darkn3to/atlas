package com.dhruv.atlas.ingestion;

import com.dhruv.atlas.ingestion.parser.HtmlContentExtractorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlContentExtractorConcurrencyTest {


    HtmlContentExtractorService extractor;

    private static final String URL =
            "https://example.com/test-article";

    private static final String HTML = """
            <!doctype html>
            <html>
            <head><title>Test Article</title></head>
            <body>
              <article>
                <h1>Test Article</h1>
                <p>This is a test article with enough meaningful text
                to allow the Readability parser to identify the main
                article content. We use this page to test concurrent
                extraction requests in our Spring Boot application.</p>
                <p>The purpose of this test is to verify that four
                concurrent requests can reach the extraction service
                while a fifth request is rejected because all permits
                are occupied. This paragraph adds more useful content.</p>
                <p>Once the requests finish, the permits should become
                available again and later requests should be able to
                proceed normally.</p>
              </article>
            </body>
            </html>
            """;

    //@Test
    void fifthRequestIsRejectedWhenFourRequestsAreInFlight()
            throws Exception {

        ExecutorService pool = Executors.newFixedThreadPool(4);

        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<?>> requests = new ArrayList<>();

        //extractor = new HtmlContentExtractorService("http://localhost:3000",4);

        try {
            // Prepare four requests without starting them yet.
            for (int i = 0; i < 4; i++) {
                requests.add(pool.submit(() -> {
                    ready.countDown();

                    // All four wait here until we release them.
                    start.await();

                    return extractor.extract(URL, HTML);
                }));
            }

            // Wait until all four worker threads are ready.
            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "The four request threads did not become ready"
            );

            // Release all four requests together.
            start.countDown();

            // Node.js has a temporary five-second delay.
            // Give the four requests time to acquire their permits.
            Thread.sleep(500);

            // The fifth request should be rejected immediately.
            assertThrows(
                    RejectedExecutionException.class,
                    () -> extractor.extract(URL, HTML)
            );

            // Confirm the original four requests finish successfully.
            for (Future<?> request : requests) {
                request.get(8, TimeUnit.SECONDS);
            }

        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }
}