package com.dhruv.atlas.ingestion.model;

import java.util.List;

public record SearchChunk(
        String textContent,
        String sourceUrl,
        List<String> sectionPath,
        String title,
        List<Float> embedding
) {
    public SearchChunk {
        sectionPath = (sectionPath == null)?List.of():List.copyOf(sectionPath);
        embedding = (embedding == null)?List.of():List.copyOf(embedding);
    }
}
