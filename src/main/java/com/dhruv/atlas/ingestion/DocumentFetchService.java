package com.dhruv.atlas.ingestion;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DocumentFetchService {
    public Document fetch(String url) throws IOException {
        try {
            return Jsoup.connect(url)
                    .userAgent("AtlasDocumentFetcher/1.0")
                    .followRedirects(true)
                    .timeout(10_000)
                    .get();
        }
        catch (IOException e) {
            throw new IOException("Unable to fetch documentation. ");
        }
    }

}

