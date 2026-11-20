package com.dhruv.atlas.service;

import co.elastic.clients.elasticsearch.nodes.Ingest;
import com.dhruv.atlas.entity.DiscoveredPage;
import com.dhruv.atlas.entity.DocumentationSection;
import com.dhruv.atlas.entity.IngestionSource;
import com.dhruv.atlas.repository.DiscoveredPageRepository;
import com.dhruv.atlas.repository.DocumentationSectionRepository;
import com.dhruv.atlas.repository.IngestionSourceRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PersistenceService {
    private final DocumentationSectionRepository documentationSectionRepository;

    public final IngestionSourceRepository ingestionSourceRepository;

    public final DiscoveredPageRepository discoveredPageRepository;

    public PersistenceService(DocumentationSectionRepository documentationSectionRepository,
                              IngestionSourceRepository ingestionSourceRepository,
                              DiscoveredPageRepository discoveredPageRepository) {
        this.documentationSectionRepository = documentationSectionRepository;
        this.ingestionSourceRepository = ingestionSourceRepository;
        this.discoveredPageRepository = discoveredPageRepository;
    }

    public IngestionSource registerIngestionSource(String name, String sourceType, String sourceUrl) {
        IngestionSource source = new IngestionSource();
        source.setName(name);
        source.setSourceType(sourceType);
        source.setSourceUrl(sourceUrl);

        return ingestionSourceRepository.save(source);
    }

    public List<IngestionSource> getIngestionSources() {
        return ingestionSourceRepository.findAllByOrderByIdAsc();
    }

    public DiscoveredPage registerDiscoveredPage(
            IngestionSource source,
            String url,
            OffsetDateTime lastModified
    ) {

        DiscoveredPage page = discoveredPageRepository
                .findBySourceAndUrl(source, url)
                .orElseGet(() -> {
                    DiscoveredPage newPage = new DiscoveredPage();
                    newPage.setSource(source);
                    newPage.setUrl(url);
                    return newPage;
                });

        page.setLastModified(lastModified);
        page.setLastSeenAt(OffsetDateTime.now(ZoneOffset.UTC));

        return discoveredPageRepository.save(page);
    }

    public void markIndexed(@NonNull DiscoveredPage page,
                            String contentHash) {
        page.setContentHash(contentHash);
        page.setLastIndexedAt(OffsetDateTime.now(ZoneOffset.UTC));

        discoveredPageRepository.save(page);
    }

    public void registerDocumentationSection(
            DiscoveredPage page,
            String heading,
            String sectionUrl,
            String contentHash
            ) {
        DocumentationSection documentationSection = new DocumentationSection();
        documentationSection.setDiscoveredPage(page);
        documentationSection.setHeading(heading);
        documentationSection.setSectionUrl(sectionUrl);
        documentationSection.setContentHash(contentHash);
        documentationSection.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        documentationSectionRepository.save(documentationSection);
    }
}
