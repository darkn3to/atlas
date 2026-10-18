package com.dhruv.atlas.ingestion.parser;

import com.dhruv.atlas.ingestion.model.ContentBlock;
import com.dhruv.atlas.ingestion.model.ContentType;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.model.links;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentationHtmlParser {
    private static final Pattern HEADING_PATTERN = Pattern.compile("h([1-6])");

    public String extractTitle(String html, Element document) {

        Element titleElement = Jsoup.parse(html).selectFirst("title");
        if (titleElement != null && !titleElement.text().isBlank()) return titleElement.text().trim();

        titleElement = document.selectFirst("h1");
        if (titleElement != null && !titleElement.text().isBlank()) return titleElement.text().trim();

        return "Untitled document";
    }

    public List<links> extractLinks(Element element) {
        List<links> links = new ArrayList<>();

        for (Element anchor : element.select("a[href]")) {
            links.add(new links(
                    anchor.text().trim(),
                    anchor.absUrl("href").isBlank()
                            ? anchor.attr("href")
                            : anchor.absUrl("href")
            ));
        }

        return links;
    }

    public String extractLanguage(Element element) {
        Element code = element.selectFirst("code");
        Element target = code != null ? code : element;

        for (String className : target.classNames()) {
            String lower = className.toLowerCase(Locale.ROOT);

            if (lower.startsWith("language-")) {
                return className.substring("language-".length());
            }

            if (lower.startsWith("lang-")) {
                return className.substring("lang-".length());
            }
        }

        if (target.hasAttr("data-lang")) {
            return target.attr("data-lang");
        }

        return null;
    }

    public String extractCode(@NonNull Element element) {
        Element code = element.selectFirst("code");

        if (code != null && code.hasText()) {
            return code.wholeText();
        }

        return element.wholeText();
    }

    public String extractTable(Element element) {
        List<String> rows = new ArrayList<>();

        for (Element row : element.select("tr")) {
            List<String> cells = new ArrayList<>();

            for (Element cell : row.children()) {
                String tag = cell.normalName();

                if (tag.equals("td") || tag.equals("th")) {
                    cells.add(cell.text().trim());
                }
            }

            if (!cells.isEmpty()) {
                rows.add(String.join(" | ", cells));
            }
        }

        return String.join("\n", rows);
    }

    public String extractList(Element element) {
        List<String> items = new ArrayList<>();

        for (Element item : element.children()) {
            if (!item.normalName().equals("li")) {
                continue;
            }
            String text = item.text().trim();
            if (!text.isEmpty()) {
                items.add("- " + text);
            }
        }

        return String.join("\n", items);
    }

    public void visit(@NonNull Element element, DocumentationPage documentationPage, List<String> headingStack) {
        String tag = element.normalName();
        Matcher matcher = HEADING_PATTERN.matcher(tag);

        if (matcher.matches()) {
            int level = Integer.parseInt(matcher.group(1));
            String heading = element.text().trim();
            if (heading.isBlank()) return;

            while (headingStack.size() >= level) {
                headingStack.removeLast();
            }
            headingStack.add(heading);

            documentationPage.blocks().add(new ContentBlock(ContentType.HEADING, heading, (short) level, headingStack, null, extractLinks(element)));
        }

        switch (tag) {
            case "pre":
                String code = extractCode(element);
                if (code.isBlank()) return;
                documentationPage.blocks().add(new ContentBlock(ContentType.CODE_BLOCK, code, (short) 0, headingStack, extractLanguage(element), extractLinks(element)));
                return;
            case "p":
                String text = element.wholeText();
                if (text.isBlank()) return;
                documentationPage.blocks().add(new ContentBlock(ContentType.PARAGRAPH, text, (short) 0, headingStack, null, extractLinks(element)));
                return;
            case "ul", "ol":
                String content = extractList(element);
                if (content.isBlank()) return;
                documentationPage.blocks().add(new ContentBlock(ContentType.LIST, content, (short) 0, headingStack, null, extractLinks(element)));
                return;
            case "table":
                String table = extractTable(element);
                if (table.isBlank()) return;
                documentationPage.blocks().add(new ContentBlock(ContentType.TABLE, table, (short) 0, headingStack, null, extractLinks(element)));
                return;
            case "a":
                if (element.hasAttr("href") && !element.attr("href").toLowerCase().startsWith("#")) {
                    documentationPage.blocks().add(new ContentBlock(ContentType.LINK, element.text().trim(), (short) 0, headingStack, null, extractLinks(element)));
                }
                return;
            case "nav":
                return;
            default: {

            }
        }

        for (Element child : element.children()) {
            visit(child, documentationPage, headingStack);
        }
    }

    public DocumentationPage parse(String sourceUrl, String html, String originalHtml) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Page URL cannot be blank. "
            );
        }

        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException(
                    "HTML must not be blank. "
            );
        }

        Element document = Jsoup.parse(html).body();
        String title = extractTitle(originalHtml, document);
        DocumentationPage documentationPage = new DocumentationPage(title, sourceUrl, new ArrayList<>());
        List<String> headingStack = new ArrayList<>();
        headingStack.add(title);

        for (Element child : document) {
            visit(child, documentationPage, headingStack);
        }

        return documentationPage;
    }
}
