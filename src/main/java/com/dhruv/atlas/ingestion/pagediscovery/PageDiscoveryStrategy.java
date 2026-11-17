package com.dhruv.atlas.ingestion.pagediscovery;

import java.util.List;

public interface PageDiscoveryStrategy {
    List<DiscoveredPage> discover();
}

