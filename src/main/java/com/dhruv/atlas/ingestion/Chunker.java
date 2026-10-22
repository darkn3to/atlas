package com.dhruv.atlas.ingestion;

import com.dhruv.atlas.ingestion.model.ContentBlock;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.model.SearchChunk;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

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
        int currentChunkSize = 0;

        StringBuilder content = new StringBuilder();

        for (ContentBlock cb : documentationPage.blocks()) {
            List<String> blockSectionPath = cb.sectionPath();

            if (currentSectionPath == null) {
                currentSectionPath = new ArrayList<>(blockSectionPath);
            }
            else if (!compareSections(currentSectionPath, blockSectionPath)) {
                chunks.add(new SearchChunk(
                        content.toString().trim(),
                        documentationPage.sourceUrl(),
                        currentSectionPath,
                        documentationPage.title()
                ));

                content = new StringBuilder();
                currentSectionPath = new ArrayList<>(blockSectionPath);
            }

            if (!content.isEmpty()) {
                content.append("\n\n");
            }
            content.append(cb.textContent());
        }

        if (currentSectionPath != null) {
            chunks.add(new SearchChunk(
                    content.toString().trim(),
                    documentationPage.sourceUrl(),
                    currentSectionPath,
                    documentationPage.title()
            ));
        }

        return chunks;
    }
}
