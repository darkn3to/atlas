package com.dhruv.atlas.ingestion.model;

import java.time.OffsetDateTime;

public record SitemapPage(
        String url,
        OffsetDateTime lastModified
) {}