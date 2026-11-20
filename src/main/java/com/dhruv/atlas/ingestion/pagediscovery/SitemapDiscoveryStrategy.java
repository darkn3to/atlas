package com.dhruv.atlas.ingestion.pagediscovery;

import com.dhruv.atlas.entity.IngestionSource;
import com.dhruv.atlas.exception.DocumentIngestionException;
import com.dhruv.atlas.service.PersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class SitemapDiscoveryStrategy implements PageDiscoveryStrategy {
    private final PersistenceService persistenceService;

    public SitemapDiscoveryStrategy(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public void discover(Consumer<DiscoveredPage> pageConsumer) {
        List<IngestionSource> sources = persistenceService.getIngestionSources();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        for (IngestionSource source : sources) {
            try (InputStream sitemapStream = URI.create(source.getSourceUrl()).toURL().openStream()) {
                log.info("Streaming sitemap: {}", source.getSourceUrl());
                parseSitemapStream(source, factory, sitemapStream, pageConsumer);
            } catch (Exception exe) {
                throw new DocumentIngestionException(
                        "Failed processing sitemap: " + source.getSourceUrl(), exe);
            }
        }
    }

    private void parseSitemapStream(
            IngestionSource source,
            @NonNull XMLInputFactory factory,
            InputStream sitemapStream,
            Consumer<DiscoveredPage> pageConsumer) throws XMLStreamException {

        XMLStreamReader reader = factory.createXMLStreamReader(sitemapStream);
        try {
            String url = null;
            String lastMod = null;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (reader.getLocalName()) {
                        case "url" -> {
                            url = null;
                            lastMod = null;
                        }

                        case "loc" -> url = reader.getElementText().trim();
                        case "lastmod" -> lastMod = reader.getElementText().trim();
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT
                        && "url".equals(reader.getLocalName())) {

                    if (url != null && !url.isBlank()) {
                        OffsetDateTime parsedLastMod = parseLastMod(lastMod);
                        pageConsumer.accept(new DiscoveredPage(source, url, parsedLastMod));
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    private OffsetDateTime parseLastMod(String lastMod) {
        if (lastMod == null || lastMod.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(lastMod);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(lastMod).atStartOfDay().atOffset(ZoneOffset.UTC);
        }
    }
}
