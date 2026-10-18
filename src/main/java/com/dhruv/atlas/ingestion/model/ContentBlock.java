package com.dhruv.atlas.ingestion.model;

import java.util.List;

public record ContentBlock(
    ContentType type,
    String textContent,
    short headingLevel,
    List<String> sectionPath,
    String language,
    List<links> contentBlockLinks
) {
    public ContentBlock {
        sectionPath = (sectionPath == null)?List.of():List.copyOf(sectionPath);
    }
}
