package evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EvaluationRunner {

    private static final String SEARCH_URL =
            "http://localhost:8080/api/search?query=";

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        HttpClient client = HttpClient.newHttpClient();

        // Read evaluation queries
        List<EvaluationQuery> queries = mapper.readValue(
                Files.readString(Path.of("src/main/java/evaluation/EvaluationQueries.json")),
                new TypeReference<>() {}
        );

        StringBuilder output = new StringBuilder();
        output.append("[\n");

        for (int i = 0; i < queries.size(); i++) {

            EvaluationQuery evaluationQuery = queries.get(i);

            String encodedQuery = URLEncoder.encode(
                    evaluationQuery.query(),
                    StandardCharsets.UTF_8
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEARCH_URL + encodedQuery))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            output.append("""
                    {
                      "queryId": %d,
                      "query": %s,
                      "results": %s
                    }
                    """.formatted(
                    evaluationQuery.id(),
                    mapper.writeValueAsString(evaluationQuery.query()),
                    response.body()
            ));

            if (i < queries.size() - 1) {
                output.append(",");
            }

            output.append("\n");
        }

        output.append("]");

        Files.writeString(
                Path.of("src/main/java/evaluation/esResults/evaluation_results_v3.json"),
                output.toString()
        );

        System.out.println(
                "Evaluation results written to src/main/java/evaluation/esResults/evaluation_results_v3.json"
        );
    }

    record EvaluationQuery(
            int id,
            String query,
            String topic
    ) {}
}