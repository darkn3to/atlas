package com.dhruv.atlas.ingestion.model;

import java.util.List;

public record SearchChunk(
        String sectionID,
        String textContent,
        String sourceUrl,
        List<String> sectionPath,
        String sectionUrl,
        String pageTitle,
        List<Float> embedding,
        List<String> identifiers
) {
    public SearchChunk {
        sectionPath = (sectionPath == null)?List.of():List.copyOf(sectionPath);
        embedding = (embedding == null)?List.of():List.copyOf(embedding);
    }
}
