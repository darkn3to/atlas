package com.dhruv.atlas.ingestion;

import com.dhruv.atlas.ingestion.embeddings.EmbeddingService;
import com.dhruv.atlas.ingestion.model.ContentBlock;
import com.dhruv.atlas.ingestion.model.ContentType;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;

import static com.dhruv.atlas.utils.Utils.generateSectionId;

@Slf4j
//@Service
public class Chunker {
    private final int maxChunkSize;

    private final EmbeddingService embeddingService;

    public Chunker(int maxChunkSize, EmbeddingService embeddingService) {
        this.maxChunkSize = maxChunkSize;
        this.embeddingService = embeddingService;
    }

    private boolean compareSections(@NonNull List<String> a, @NonNull List<String> b) {
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
        }

        return true;
    }

    private @NonNull SearchChunk buildChunk(
            @NonNull DocumentationPage page,
            @NonNull StringBuilder contentBuffer,
            List<String> sectionPath,
            String sectionUrl,
            List<String> identifiers) {
        String text = contentBuffer.toString().trim();
        List<Float> embedding = embeddingService.generateEmbeddings(text);
        String finalSectionUrl = (sectionUrl != null) ? page.sourceUrl() + sectionUrl : null;

        log.debug("Processing heading: {}", sectionPath.getLast());

        String sectionId = generateSectionId(page.sourceUrl(), sectionPath);

        return new SearchChunk(
                sectionId,
                text,
                page.sourceUrl(),
                sectionPath,
                finalSectionUrl,
                page.title(),
                embedding,
                identifiers
        );
    }

    public List<SearchChunk> performChunking(@NonNull DocumentationPage documentationPage) {
        List<SearchChunk> chunks = new ArrayList<>();
        List<String> currentSectionPath = null;
        String currentSectionUrl = null;

        StringBuilder contentBuffer = new StringBuilder();
        List<String> identifiers = new ArrayList<>();

        for (ContentBlock cb : documentationPage.blocks()) {
            List<String> blockSectionPath = cb.sectionPath();

            boolean sectionChanged = (currentSectionPath != null)
                    && !compareSections(currentSectionPath, blockSectionPath);

            if (sectionChanged && !contentBuffer.isEmpty()) {
                chunks.add(buildChunk(documentationPage, contentBuffer, currentSectionPath, currentSectionUrl, identifiers));
                contentBuffer = new StringBuilder();
                identifiers = new ArrayList<>();
                currentSectionUrl = null;
            }

            if (sectionChanged || currentSectionPath == null) {
                currentSectionPath = new ArrayList<>(blockSectionPath);
            }

            if (cb.sectionUrl() != null && !cb.sectionUrl().isEmpty()) {
                currentSectionUrl = cb.sectionUrl();
            }
            if (cb.identifiers() != null && !cb.identifiers().isEmpty()) {
                identifiers.addAll(cb.identifiers());
            }

            if (cb.type() == ContentType.HEADING) {
                continue;
            }

            String blockContent = cb.textContent();
            int separatorLength = contentBuffer.isEmpty() ? 0 : 2;
            boolean sizeExceeded = (contentBuffer.length() + separatorLength + blockContent.length()) > maxChunkSize;

            if (!contentBuffer.isEmpty() && sizeExceeded) {
                chunks.add(buildChunk(documentationPage, contentBuffer, currentSectionPath, currentSectionUrl, identifiers));
                contentBuffer = new StringBuilder();
                identifiers = new ArrayList<>();
            }

            if (!contentBuffer.isEmpty()) {
                contentBuffer.append("\n\n");
            }
            contentBuffer.append(blockContent);
        }

        if (!contentBuffer.isEmpty()) {
            chunks.add(buildChunk(documentationPage, contentBuffer, currentSectionPath, currentSectionUrl, identifiers));
        }

        return chunks;
    }
}
