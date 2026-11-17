package com.dhruv.atlas.ingestion.pagediscovery;

import java.time.OffsetDateTime;

public record DiscoveredPage(
        String url,
        OffsetDateTime lastMod
) {}
