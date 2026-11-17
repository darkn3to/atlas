
package com.dhruv.atlas.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SearchEvaluationRunner {

    private static final String SEARCH_ENDPOINT =
            "http://localhost:8080/api/hybridSearch?query=";

    private static final int K = 10;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        JsonNode dataset;

        try (InputStream input = SearchEvaluationRunner.class
                .getResourceAsStream("/search-evaluation_hybrid.json")) {

            if (input == null) {
                throw new IllegalStateException(
                        "Missing search-evaluation_hybrid.json in test resources");
            }

            dataset = MAPPER.readTree(input);
        }

        double totalNdcg = 0;
        double totalMrr = 0;
        double totalRecall = 0;
        int evaluatedQueries = 0;

        for (JsonNode query : dataset.path("queries")) {
            String queryId = query.path("id").asText();
            String queryText = query.path("text").asText();
            JsonNode judgments = query.path("judgments");

            if (!judgments.isArray() || judgments.isEmpty()) {
                System.out.printf(
                        "%s | SKIPPED: no relevance judgments%n", queryId);
                continue;
            }

            JsonNode response = search(queryText);
            JsonNode results = response.path("results");

            if (!results.isArray()) {
                throw new IllegalStateException(
                        "Response has no results array for: " + queryText);
            }

            Map<String, Integer> relevanceBySection =
                    buildJudgments(judgments);

            // Deduplicate sections, retaining their first-ranked chunk.
            List<JsonNode> uniqueResults = new ArrayList<>();
            Set<String> seenSections = new HashSet<>();

            for (JsonNode result : results) {
                String sectionKey = sectionKey(
                        result.path("url").asText(""),
                        result.path("sectionPath").asText());

                if (seenSections.add(sectionKey)) {
                    uniqueResults.add(result);
                }
            }

            double ndcg = ndcg(
                    uniqueResults, relevanceBySection, judgments);

            double mrr = mrr(uniqueResults, relevanceBySection);

            double recall = recall(
                    uniqueResults, relevanceBySection);

            System.out.printf(
                    Locale.ROOT,
                    "%s | NDCG@%d=%.4f | MRR=%.4f | Recall@%d=%.4f%n",
                    queryId, K, ndcg, mrr, K, recall);

            totalNdcg += ndcg;
            totalMrr += mrr;
            totalRecall += recall;
            evaluatedQueries++;
        }

        if (evaluatedQueries == 0) {
            System.out.println("No queries were evaluated.");
            return;
        }

        System.out.println("\n=== Evaluation Summary ===");
        System.out.printf(Locale.ROOT, "Queries: %d%n", evaluatedQueries);
        System.out.printf(Locale.ROOT, "Mean NDCG@%d: %.4f%n",
                K, totalNdcg / evaluatedQueries);
        System.out.printf(Locale.ROOT, "Mean MRR: %.4f%n",
                totalMrr / evaluatedQueries);
        System.out.printf(Locale.ROOT, "Mean Recall@%d: %.4f%n",
                K, totalRecall / evaluatedQueries);
    }

    private static JsonNode search(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(
                query, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SEARCH_ENDPOINT + encodedQuery))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Search API returned HTTP "
                            + response.statusCode()
                            + " for query: " + query);
        }

        return MAPPER.readTree(response.body());
    }

    private static Map<String, Integer> buildJudgments(
            JsonNode judgments) {

        Map<String, Integer> relevance = new HashMap<>();

        for (JsonNode judgment : judgments) {
            String path = judgment.path("sectionPath").asText();
            String url = judgment.path("url").asText("");

            if (path.isBlank()) {
                throw new IllegalArgumentException(
                        "Every judgment needs a sectionPath");
            }

            int score = judgment.path("relevance").asInt(-1);

            if (score < 0 || score > 3) {
                throw new IllegalArgumentException(
                        "Relevance must be between 0 and 3: " + path);
            }

            String key = judgmentKey(url, path);

            if (relevance.putIfAbsent(key, score) != null) {
                throw new IllegalArgumentException(
                        "Duplicate judgment for section: " + path);
            }
        }

        return relevance;
    }

    private static String sectionKey(String url, String path) {
        return url + "\n" + path;
    }

    private static String judgmentKey(String url, String path) {
        // Allows the current dataset to omit URLs.
        return url.isBlank() ? path : sectionKey(url, path);
    }

    private static int relevance(
            JsonNode result,
            Map<String, Integer> judgments) {

        String path = result.path("sectionPath").asText();
        String url = result.path("url").asText("");

        Integer score = judgments.get(sectionKey(url, path));

        if (score == null) {
            score = judgments.get(path);
        }

        // Unjudged results do not receive relevance credit.
        return score == null ? 0 : score;
    }

    private static double dcg(
            List<JsonNode> results,
            Map<String, Integer> judgments) {

        double sum = 0;

        for (int i = 0; i < Math.min(K, results.size()); i++) {
            int rel = relevance(results.get(i), judgments);

            sum += (Math.pow(2, rel) - 1)
                    / (Math.log(i + 2) / Math.log(2));
        }

        return sum;
    }

    private static double ndcg(
            List<JsonNode> results,
            Map<String, Integer> judgments,
            JsonNode rawJudgments) {

        double actualDcg = dcg(results, judgments);

        List<Integer> idealScores = new ArrayList<>();

        for (JsonNode judgment : rawJudgments) {
            idealScores.add(judgment.path("relevance").asInt());
        }

        idealScores.sort(Comparator.reverseOrder());

        double idealDcg = 0;

        for (int i = 0; i < Math.min(K, idealScores.size()); i++) {
            int rel = idealScores.get(i);

            idealDcg += (Math.pow(2, rel) - 1)
                    / (Math.log(i + 2) / Math.log(2));
        }

        return idealDcg == 0 ? 0 : actualDcg / idealDcg;
    }

    private static double mrr(
            List<JsonNode> results,
            Map<String, Integer> judgments) {

        for (int i = 0; i < Math.min(K, results.size()); i++) {
            if (relevance(results.get(i), judgments) > 0) {
                return 1.0 / (i + 1);
            }
        }

        return 0;
    }

    private static double recall(
            List<JsonNode> results,
            Map<String, Integer> judgments) {

        long relevantCount = judgments.values().stream()
                .filter(score -> score > 0)
                .count();

        if (relevantCount == 0) {
            return 0;
        }

        Set<String> foundRelevant = new HashSet<>();

        for (int i = 0; i < Math.min(K, results.size()); i++) {
            JsonNode result = results.get(i);

            if (relevance(result, judgments) > 0) {
                foundRelevant.add(sectionKey(
                        result.path("url").asText(""),
                        result.path("sectionPath").asText()));
            }
        }

        return (double) foundRelevant.size() / relevantCount;
    }
}

