package com.dhruv.atlas.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public class Utils {
    public static boolean hasCause(
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

    public static @NonNull Document fetchDocument(String url) throws IOException {
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
