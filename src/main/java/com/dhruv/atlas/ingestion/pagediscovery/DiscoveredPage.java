package com.dhruv.atlas.ingestion.pagediscovery;

import com.dhruv.atlas.entity.IngestionSource;

import java.time.OffsetDateTime;

public record DiscoveredPage(
        IngestionSource source,
        String url,
        OffsetDateTime lastMod
) {}
