/*package com.dhruv.atlas;

import java.io.File;
import java.io.IOException;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.transport.TransportUtils;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLContext;

class devDocs {
    String title, content, source;

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

public class Main {
    public static void main(String[] args) throws IOException {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        String serverUrl = "https://localhost:9200";

        ClassPathResource resource = new ClassPathResource("http_ca.crt");

        File certFile = resource.getFile();

        SSLContext sslContext = TransportUtils
                .sslContextFromHttpCaCrt(certFile);

        ElasticsearchClient esClient = ElasticsearchClient.of(b -> b
                .host(serverUrl)
                .usernameAndPassword("elastic", "elastic")
                .sslContext(sslContext)
        );

        try {
            SearchResponse<devDocs> a = esClient.search(s -> s
                .index("developer_docs")
                .query(q -> q
                    .term(t -> t
                        .field("content")
                        .value(v -> v.stringValue("data"))
                    )
                ), devDocs.class);

            for (var hit : a.hits().hits()) {
                devDocs doc = hit.source();
                System.out.println(doc.getTitle());
                System.out.println(doc.getContent());
                System.out.println(doc.getSource());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            esClient.close();
        }
    }
    package com.dhruv.atlas.ingestion;

import com.wuman.jreadability.Readability;
import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.springframework.stereotype.Service;

@Service
public class HtmlContentExtractor {
    public record ExtractedContent(

            String html
    ) {}

    public String extract(String url, String html) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Page URL cannot be blank. ");
        }

        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("HTML must not be blank. ");
        }

        //Readability readability = new Readability();

        Readability readability = new Readability(html);
        readability.init();

        /*if (article.getContent() == null) {
            throw new IllegalStateException("Readability couldn't extract content from: " + url + ". ");
        }*/

        /*return new ExtractedContent(
                article.getTitle(),
                article.getContent(),
                article.getTextContent(),
                article.getUri()
        );
        return readability.outerHtml();
    }
            }*/







