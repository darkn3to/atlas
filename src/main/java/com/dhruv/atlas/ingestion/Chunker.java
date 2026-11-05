package com.dhruv.atlas.ingestion;

import com.dhruv.atlas.ingestion.embeddings.EmbeddingService;
import com.dhruv.atlas.ingestion.model.ContentBlock;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
//@Service
public class Chunker {
    private final int maxChunkSize;

    private final EmbeddingService embeddingService;

    public Chunker(int maxChunkSize, EmbeddingService embeddingService) {
        this.maxChunkSize = maxChunkSize;
        this.embeddingService = embeddingService;
    }

    private boolean compareSections(List<String> a, List<String> b) {
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
        }

        return true;
    }

    public List<SearchChunk> performChunking(@NonNull DocumentationPage documentationPage) {
        List<SearchChunk> chunks = new ArrayList<>();
        List<String> currentSectionPath = null;

        StringBuilder contentBuffer = new StringBuilder();
        List<String> identifiers = new ArrayList<>();

        for (ContentBlock cb : documentationPage.blocks()) {
            List<String> blockSectionPath = cb.sectionPath();
            String blockContent = cb.textContent();

            boolean sectionChanged = (currentSectionPath != null)
                    && !compareSections(currentSectionPath, blockSectionPath);

            int separatorLength = (contentBuffer.isEmpty()) ? 0 : 2;
            int proposedSize = contentBuffer.length()
                    + separatorLength
                    + blockContent.length();

            boolean sizeExceeded = proposedSize > maxChunkSize;

            if (!contentBuffer.isEmpty() && (sectionChanged || sizeExceeded)) {
                List<Float> embedding = embeddingService.generateEmbeddings(contentBuffer.toString());

                //log.info(String.valueOf(embedding.size()));
                //if (!identifiers.isEmpty()) System.out.println(identifiers);
                chunks.add(new SearchChunk(
                        contentBuffer.toString().trim(),
                        documentationPage.sourceUrl(),
                        currentSectionPath,
                        documentationPage.title(),
                        embedding,
                        identifiers
                ));

                contentBuffer = new StringBuilder();
                identifiers = new ArrayList<>();
            }

            if (contentBuffer.isEmpty()) {
                currentSectionPath = new ArrayList<>(blockSectionPath);
            } else {
                contentBuffer.append("\n\n");
            }

            contentBuffer.append(blockContent);
            if (cb.identifiers() != null && !cb.identifiers().isEmpty()) {
                identifiers.addAll(cb.identifiers());
            }
        }

        if (!contentBuffer.isEmpty()) {
            List<Float> embedding = embeddingService.generateEmbeddings(contentBuffer.toString());
            //log.info(String.valueOf(embedding.size()));
            //if (!identifiers.isEmpty()) System.out.println(identifiers);
            chunks.add(new SearchChunk(
                    contentBuffer.toString().trim(),
                    documentationPage.sourceUrl(),
                    currentSectionPath,
                    documentationPage.title(),
                    embedding,
                    identifiers
            ));
        }

        return chunks;
    }
}
