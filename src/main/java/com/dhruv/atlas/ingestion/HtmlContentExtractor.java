package com.dhruv.atlas.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Service
public class HtmlContentExtractor {

    private final RestClient restClient;

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

    public ExtractedContent extract(String url, String html) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "Page URL cannot be blank."
            );
        }

        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException(
                    "HTML must not be blank."
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
                        "Readability couldn't extract content from: " + url
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
                            + e.getStatusCode().value() + " for: " + url,
                    e
            );
        } catch (RestClientException e) {
            if (hasCause(e, SocketTimeoutException.class)) {
                throw new IllegalStateException(
                        "Timed out while communicating with Readability service for: "
                                + url, e
                );
            }

            if (hasCause(e, ConnectException.class)) {
                throw new IllegalStateException(
                        "Could not connect to Readability service for: " + url
                                + ". Ensure the service is running and reachable.",
                        e
                );
            }

            throw new IllegalStateException(
                    "Communication with Readability service failed for: " + url,
                    e
            );
        }
    }
}