package com.dhruv.atlas.ingestion.parser;

import com.dhruv.atlas.ingestion.model.ContentBlock;
import com.dhruv.atlas.ingestion.model.ContentType;
import com.dhruv.atlas.ingestion.model.DocumentationPage;
import com.dhruv.atlas.ingestion.model.links;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DocumentationHtmlParserService {
    private static final Pattern HEADING_PATTERN =
            Pattern.compile("h([1-6])");

    private static final Pattern CODE_PATTERN =
            Pattern.compile("`{1,3}([^`]+)`{1,3}");

    private static final Pattern ANNOTATION_PATTERN =
            Pattern.compile("@[A-Za-z_$][\\w$]*");

    private static final Pattern QUALIFIED_NAME_PATTERN =
            Pattern.compile(
                    "\\b(?:[a-z_$][\\w$]*\\.)+[A-Z_$][\\w$]*\\b"
            );

    private static final Pattern CAMEL_CASE_PATTERN =
            Pattern.compile("\\b[a-z][a-zA-Z0-9_$]*[A-Z][a-zA-Z0-9_$]*\\b");

    private static final Pattern PASCAL_CASE_PATTERN =
            Pattern.compile("\\b[A-Z][a-z0-9_$]*[A-Z][a-zA-Z0-9_$]*\\b");

    private static final Pattern UPPERCASE_TOKEN_PATTERN =
            Pattern.compile("\\b[A-Z]{3,}[A-Z0-9_-]*\\b");

    public String extractTitle(String html, Element document) {

        Element titleElement = Jsoup.parse(html).selectFirst("title");
        if (titleElement != null && !titleElement.text().isBlank()) return titleElement.text().trim();

        titleElement = document.selectFirst("h1");
        if (titleElement != null && !titleElement.text().isBlank()) return titleElement.text().trim();

        return "Untitled document";
    }

    public List<links> extractLinks(@NonNull Element element) {
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

    public String extractSectionLinks(@NonNull Element element) {
        Element link = element.selectFirst("a");

        if (link == null) {
            return null;
        }

        /*System.out.println(element.text());
        System.out.println(link.attr("href"));
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();*/

        return link.attr("href");
    }

    public String extractLanguage(@NonNull Element element) {
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

    public String extractTable(@NonNull Element element) {
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

    public String extractList(@NonNull Element element) {
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

    private boolean isValidIdentifier(String candidate) {
        return candidate != null
                && !candidate.isBlank()
                && candidate.length() >= 3
                && candidate.length() <= 100
                /*&& !candidate.matches(".*\\s+.*")*/;
    }

    private void matchIdentifiers(String content, Set<String> extractedIdentifiers, @NonNull Pattern pattern) {
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String candidate = (matcher.groupCount() > 0 && matcher.group(1) != null && pattern == CODE_PATTERN)
                    ? matcher.group(1)
                    : matcher.group();

            candidate = candidate.trim();

            if (isValidIdentifier(candidate)) {
                extractedIdentifiers.add(candidate);
            }
        }
    }

    public List<String> extractIdentifiersFromUserQuery(@NonNull String content) {
        Set<String> extractedIdentifiers = new LinkedHashSet<>();

        matchIdentifiers(content, extractedIdentifiers, CODE_PATTERN);
        matchIdentifiers(content, extractedIdentifiers, ANNOTATION_PATTERN);
        matchIdentifiers(content, extractedIdentifiers, QUALIFIED_NAME_PATTERN);
        matchIdentifiers(content, extractedIdentifiers, UPPERCASE_TOKEN_PATTERN);
        matchIdentifiers(content, extractedIdentifiers, CAMEL_CASE_PATTERN);
        matchIdentifiers(content, extractedIdentifiers, PASCAL_CASE_PATTERN);

        return List.copyOf(extractedIdentifiers);
    }

    public List<String> extractIdentifiers(@NonNull Element element) {
        Set<String> extractedIdentifiers = new LinkedHashSet<>();

        String tag = element.normalName();

        if (tag.equals("p") || (tag.equals("a") && element.hasAttr("href"))) {
            String text = element.text();
            matchIdentifiers(text, extractedIdentifiers, CODE_PATTERN);
            matchIdentifiers(text, extractedIdentifiers, ANNOTATION_PATTERN);
            matchIdentifiers(text, extractedIdentifiers, QUALIFIED_NAME_PATTERN);
            matchIdentifiers(text, extractedIdentifiers, UPPERCASE_TOKEN_PATTERN);
            matchIdentifiers(text, extractedIdentifiers, CAMEL_CASE_PATTERN);
            matchIdentifiers(text, extractedIdentifiers, PASCAL_CASE_PATTERN);
        }

        for (Element code : element.select("code")) {
            boolean hasPreAncestor = code.parents().stream()
                    .anyMatch(p -> "pre".equalsIgnoreCase(p.normalName()));

            if (hasPreAncestor) {
                continue;
            }

            matchIdentifiers(code.wholeText(), extractedIdentifiers, CODE_PATTERN);
            matchIdentifiers(code.wholeText(), extractedIdentifiers, ANNOTATION_PATTERN);
            matchIdentifiers(code.wholeText(), extractedIdentifiers, QUALIFIED_NAME_PATTERN);
            matchIdentifiers(code.wholeText(), extractedIdentifiers, UPPERCASE_TOKEN_PATTERN);
            matchIdentifiers(code.wholeText(), extractedIdentifiers, CAMEL_CASE_PATTERN);
            matchIdentifiers(code.wholeText(), extractedIdentifiers, PASCAL_CASE_PATTERN);
        }

        for (Element link : element.select("a[href]")) {
            matchIdentifiers(link.text(), extractedIdentifiers, CAMEL_CASE_PATTERN);
            matchIdentifiers(link.text(), extractedIdentifiers, UPPERCASE_TOKEN_PATTERN);
            matchIdentifiers(link.text(), extractedIdentifiers, PASCAL_CASE_PATTERN);
        }

        return List.copyOf(extractedIdentifiers);
    }

    private Set<String> extractHeadingIdentifiers(@NonNull List<String> headings) {
        Set<String> extractedIdentifiers = new LinkedHashSet<>();

        for (String heading : headings) {
            matchIdentifiers(heading, extractedIdentifiers, CAMEL_CASE_PATTERN);
            matchIdentifiers(heading, extractedIdentifiers, UPPERCASE_TOKEN_PATTERN);
            matchIdentifiers(heading, extractedIdentifiers, PASCAL_CASE_PATTERN);
        }

        return extractedIdentifiers;
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

            Set<String> headingIdentifiers = extractHeadingIdentifiers(headingStack);
            List<String> identifiers = extractIdentifiers(element);
            headingIdentifiers.addAll(identifiers);

            documentationPage.blocks().add(new ContentBlock(ContentType.HEADING, heading, (short) level, new ArrayList<>(headingStack), extractSectionLinks(element), null, extractLinks(element), List.copyOf(headingIdentifiers)));
            return;
        }

        switch (tag) {
            case "code":
                String code = extractCode(element);
                if (code.isBlank()) return;
                documentationPage.blocks().add(new ContentBlock(ContentType.CODE_BLOCK, code, (short) 0, new ArrayList<>(headingStack), null, extractLanguage(element), extractLinks(element), extractIdentifiers(element)));
                return;
            case "p":
                String text = element.wholeText();
                if (text.isBlank()) return;
                documentationPage.blocks().add(new ContentBlock(ContentType.PARAGRAPH, text, (short) 0, new ArrayList<>(headingStack), null, null, extractLinks(element), extractIdentifiers(element)));
                return;
            case "ul", "ol":
                String content = extractList(element);
                if (content.isBlank()) return;
                documentationPage.blocks().add(new ContentBlock(ContentType.LIST, content, (short) 0, new ArrayList<>(headingStack), null, null, extractLinks(element), extractIdentifiers(element)));
                return;
            case "table":
                String table = extractTable(element);
                if (table.isBlank()) return;
                documentationPage.blocks().add(new ContentBlock(ContentType.TABLE, table, (short) 0, new ArrayList<>(headingStack), null, null, extractLinks(element), extractIdentifiers(element)));
                return;
            case "a":
                if (element.hasAttr("href") && !element.attr("href").toLowerCase().startsWith("#")) {
                    documentationPage.blocks().add(new ContentBlock(ContentType.LINK, element.text().trim(), (short) 0, new ArrayList<>(headingStack), null, null, extractLinks(element), extractIdentifiers(element)));
                }
                return;
            case "pre":
            case "nav":
                return;
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

        for (Element child : document.children()) {
            visit(child, documentationPage, headingStack);
        }

        return documentationPage;
    }
}