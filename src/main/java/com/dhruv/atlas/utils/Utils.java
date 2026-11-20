package com.dhruv.atlas.utils;

import com.dhruv.atlas.exception.DocumentIngestionException;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import com.dhruv.atlas.ingestion.pagediscovery.DiscoveredPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

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

    public static @NonNull Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .maxBodySize(0)
                    .userAgent("AtlasDocumentFetcher/1.0")
                    .followRedirects(true)
                    .timeout(10_000)
                    .get();
        }
        catch (IOException e) {
            throw new DocumentIngestionException("Unable to fetch documentation/xml resource: " + url + ". ", e);
        }
    }

    public static String generateHash(String... fields) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (DataOutputStream out = new DataOutputStream(buffer)) {
                for (String field : fields) {
                    writeField(out, field);
                }
            }

            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(buffer.toByteArray());

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate hash", e);
        }
    }

    private static void writeField(@NonNull DataOutputStream out, @NonNull String field)
            throws IOException {
        byte[] bytes = field.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public static String generateChunkId(@NonNull SearchChunk chunk) {
        return Utils.generateHash(
                chunk.sourceUrl(),
                String.join("", chunk.sectionPath()),
                chunk.textContent()
        );
    }

    public static String generateSectionId(@NonNull String sourceUrl, List<String> sectionPath) {
        return Utils.generateHash(
                sourceUrl,
                String.join("", sectionPath)
        );
    }
}
