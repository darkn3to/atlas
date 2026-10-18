package com.dhruv.atlas.ingestion.model;

import java.util.List;

public record DocumentationPage(
    String title,
    String sourceUrl,
    List<ContentBlock> blocks
) {}