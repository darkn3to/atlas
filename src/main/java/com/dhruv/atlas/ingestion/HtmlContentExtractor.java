package com.dhruv.atlas.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Slf4j
@Service
public class HtmlContentExtractor {

    private final RestClient restClient, healthClient;
    private static final long IDLE_THRESHOLD_NANOS = Duration.ofMinutes(5).toNanos();
    private volatile long serviceLastUsed = 0;

    public HtmlContentExtractor(
            @Value("${readability.service.url:http://localhost:3000}")
            String baseUrl
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(requestFactory)
                    .build();

        var healthRequestFactory = new SimpleClientHttpRequestFactory();
        healthRequestFactory.setConnectTimeout(Duration.ofSeconds(2));
        healthRequestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.healthClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(healthRequestFactory)
                .build();
    }

    public record ExtractedContent(
            String title,
            String html,
            String text,
            String sourceUrl
    ) {}

    private record ExtractionRequest(
            String url,
            String html
    ) {}

    private record ExtractionResponse(
            String title,
            String content,
            String textContent
    ) {}

    private static boolean hasCause(
            Throwable error,
            Class<? extends Throwable> expectedType
    ) {
        Throwable current = error;

        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            Throwable next = current.getCause();
            if (next == current) {
                break;
            }
            current = next;
        }
        return false;
    }

    private void coldStartService() {
        log.info("Cold starting Readability service. ");
        short maxAttempts = 20, attempts = 1;
        while (true) {
            try {
                healthClient.get()
                        .uri("/health")
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientException ex) {
                log.info("Attempt {} Retrying to connect with Readability service. ", attempts++);
                if (attempts == maxAttempts) {
                    throw new IllegalStateException("Could not connect to Readability service even after " + maxAttempts + " retries. ", ex);
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for Readability service. ", e);
                }
            }
        }
    }

    public ExtractedContent extract(String url, String html) {
        if (serviceLastUsed == 0 || System.nanoTime() - serviceLastUsed >= IDLE_THRESHOLD_NANOS) {
            coldStartService();
            serviceLastUsed = System.nanoTime();
        }

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "Page URL cannot be blank. "
            );
        }

        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException(
                    "HTML must not be blank. "
            );
        }

        ExtractionResponse article;

        try {
            article = restClient.post()
                    .uri("/extract")
                    .body(new ExtractionRequest(url, html))
                    .retrieve()
                    .body(ExtractionResponse.class);

            if (article == null || article.content() == null) {
                throw new IllegalStateException(
                        "Readability service couldn't extract content from: " + url + ". "
                );
            }

            return new ExtractedContent(
                    article.title(),
                    article.content(),
                    article.textContent(),
                    url
            );
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Readability service returned HTTP "
                            + e.getStatusCode().value() + " for: " + url + ". ",
                    e
            );
        } catch (RestClientException e) {
            if (hasCause(e, SocketTimeoutException.class)) {
                throw new IllegalStateException(
                        "Timed out while communicating with Readability service for: "
                                + url + ". ", e
                );
            }

            if (hasCause(e, ConnectException.class)) {
                throw new IllegalStateException(
                        "Could not connect to Readability service for: " + url
                                + ". Ensure the service is running and reachable. ",
                        e
                );
            }

            throw new IllegalStateException(
                    "Communication with Readability service failed for: " + url + ". ",
                    e
            );
        }
    }
}