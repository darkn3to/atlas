package com.dhruv.atlas.ingestion;

import com.dhruv.atlas.ingestion.model.ContentBlock;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.parser.DocumentationHtmlParser;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

class DocumentationHtmlParserTest {

    private final DocumentationHtmlParser parser =
            new DocumentationHtmlParser();

    @Test
    void shouldPreserveDocumentStructureAndSectionContext() throws IOException {

        String url = "https://redis.io/docs/latest/develop/get-started/search-tutorial/vector-search";

        var extractor = new HtmlContentExtractor("https://readability-js-cwg4g4gcg9c2amb4.centralindia-01.azurewebsites.net");
        DocumentFetchService documentFetchService = new DocumentFetchService();
        Document document = documentFetchService.fetch(url);
        var result = extractor.extract(url, document.html());

        DocumentationPage page = parser.parse(url, result.html(), document.html());

        System.out.println("Title: " + page.title());

        try (PrintWriter writer = new PrintWriter("output.txt")) {
            for (int i = 0; i < page.blocks().size(); i++) {
                ContentBlock block = page.blocks().get(i);

                String text = String.format(
                        "%d | %s | section=%s | language=%s",
                        i,
                        block.type(),
                        block.sectionPath(),
                        block.language()
                ) + "\n" + block.textContent();

                String[] lines = text.split("\\R", -1);

                int maxWidth = 0;
                for (String line : lines) {
                    maxWidth = Math.max(maxWidth, line.length());
                }

                String border = "+" + "-".repeat(maxWidth + 2) + "+";

                writer.println(border);

                for (String line : lines) {
                    writer.printf("| %-"+ maxWidth + "s |%n", line);
                }

                writer.println(border);
                writer.println();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}