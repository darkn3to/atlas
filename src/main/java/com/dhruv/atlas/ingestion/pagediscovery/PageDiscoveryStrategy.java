package com.dhruv.atlas.ingestion.pagediscovery;

import java.util.List;
import java.util.function.Consumer;

public interface PageDiscoveryStrategy {
    void discover(Consumer<DiscoveredPage> pageConsumer);
}

