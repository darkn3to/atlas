package com.dhruv.atlas.ingestion.pagediscovery;

import com.dhruv.atlas.exception.DocumentIngestionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SitemapDiscoveryStrategy implements PageDiscoveryStrategy {
    @Override
    public List<DiscoveredPage> discover() {
        List<DiscoveredPage> discoveredPages = new ArrayList<>();
        try (InputStream input = SitemapDiscoveryStrategy.class.getResourceAsStream("/feeder.xml")) {

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            try {
                org.w3c.dom.Document sitemapDoc = documentBuilder.parse(input);
                NodeList sitemapList = sitemapDoc.getElementsByTagName("SiteMapUrl");

                for (int i = 0; i < sitemapList.getLength(); i++) {
                    Node node = sitemapList.item(i);

                    String xmlUrl = node.getTextContent().trim();

                    org.w3c.dom.Document doc;
                    try (InputStream sitemapStream = URI.create(xmlUrl).toURL().openStream()) {
                        doc = documentBuilder.parse(sitemapStream);
                    } catch (Exception exe) {
                        throw new DocumentIngestionException(exe.getMessage(), exe);
                    }

                    NodeList nodeList = doc.getElementsByTagName("url");

                    for (int j = 0; j < nodeList.getLength(); j++) {
                        Node urlNode = nodeList.item(j);

                        NodeList children = urlNode.getChildNodes();

                        String url = null, lastMod = null;

                        for (int k = 0; k < children.getLength(); k++) {

                            Node child = children.item(k);

                            if (child.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            }

                            switch (child.getNodeName()) {
                                case "loc" ->
                                        url = child.getTextContent().trim();

                                case "lastmod" ->
                                        lastMod = child.getTextContent().trim();
                            }
                        }

                        if (url != null && !url.isBlank() && url.endsWith(".html")) {
                            OffsetDateTime parsedLastMod =
                                    (lastMod != null && !lastMod.isBlank())
                                            ? OffsetDateTime.parse(lastMod)
                                            : null;

                            discoveredPages.add(new DiscoveredPage(url, parsedLastMod));
                        }
                    }
                }
            } catch (IOException | SAXException ex) {
                String message = "Unable to parse xml from feeder.xml .";
                log.error(message);
                throw new DocumentIngestionException(message, ex);
            }
        } catch (IOException e) {
            String message = "Unable to find feeder.xml . ";
            log.error(message);
            throw new DocumentIngestionException(message, e);
        } catch (ParserConfigurationException e) {
            String message = "DocumentBuilder cannot be created which satisfies the configuration requested. ";
            log.error(message);
            throw new DocumentIngestionException(message, e);
        }

        return discoveredPages;
    }
}
