package com.dhruv.atlas.ingestion;

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

    public Chunker(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
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

        for (ContentBlock cb : documentationPage.blocks()) {
            List<String> blockSectionPath = cb.sectionPath();
            String blockContent = cb.textContent();

            boolean sectionChanged = currentSectionPath != null
                    && !compareSections(currentSectionPath, blockSectionPath);

            int separatorLength = (contentBuffer.isEmpty()) ? 0 : 2;
            int proposedSize = contentBuffer.length()
                    + separatorLength
                    + blockContent.length();

            boolean sizeExceeded = proposedSize > maxChunkSize;

            if (!contentBuffer.isEmpty() && (sectionChanged || sizeExceeded)) {
                chunks.add(new SearchChunk(
                        contentBuffer.toString().trim(),
                        documentationPage.sourceUrl(),
                        currentSectionPath,
                        documentationPage.title()
                ));

                contentBuffer = new StringBuilder();
            }

            if (contentBuffer.isEmpty()) {
                currentSectionPath = new ArrayList<>(blockSectionPath);
            } else {
                contentBuffer.append("\n\n");
            }

            contentBuffer.append(blockContent);
        }

        if (!contentBuffer.isEmpty()) {
            chunks.add(new SearchChunk(
                    contentBuffer.toString().trim(),
                    documentationPage.sourceUrl(),
                    currentSectionPath,
                    documentationPage.title()
            ));
        }

        return chunks;
    }
}
