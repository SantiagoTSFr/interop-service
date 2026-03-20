package com.interop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interop.model.Case;
import com.interop.service.SearchIndexService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 2: Search determinism.
 *
 * Same query + same data ⇒ identical ordered top_k results every single time.
 *
 * Additional scenarios:
 *  A) Repeated calls return exact same result list (order + scores).
 *  B) Different queries return different results.
 *  C) Index rebuild with same data yields same results.
 *  D) Empty query returns 400.
 *  E) Index with zero docs returns empty list gracefully.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SearchDeterminismTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired SearchIndexService searchIndex;
    @Autowired ObjectMapper objectMapper;

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private JsonNode search(String query, int topK) {
        String body = String.format("{\"query\":\"%s\",\"top_k\":%d}", query, topK);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/search", new HttpEntity<>(body, headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resultsSignature(JsonNode response) {
        // Build a string like "caseId:score,caseId:score,..." for easy comparison
        StringBuilder sb = new StringBuilder();
        for (JsonNode r : response.path("results")) {
            sb.append(r.path("case_id").asText())
              .append(":")
              .append(r.path("score").asText())
              .append(",");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("A: Same query returns identical ordered results on repeated calls")
    void sameQueryReturnsSameResults() {
        String query = "AML compliance fraud billing reconciliation";
        int topK = 5;

        // Run the same query 5 times
        List<String> signatures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            JsonNode result = search(query, topK);
            signatures.add(resultsSignature(result));
        }

        // All signatures must be identical
        String first = signatures.get(0);
        assertThat(first).isNotEmpty(); // at least some results
        for (String sig : signatures) {
            assertThat(sig).isEqualTo(first);
        }
    }

    @Test
    @Order(2)
    @DisplayName("A2: Scores are consistent and top result is highest scorer")
    void scoresAreOrderedDescending() {
        JsonNode result = search("fraud detection alert account", 10);
        JsonNode results = result.path("results");

        assertThat(results.size()).isGreaterThan(0);

        double prevScore = Double.MAX_VALUE;
        for (JsonNode r : results) {
            double score = r.path("score").asDouble();
            assertThat(score).isLessThanOrEqualTo(prevScore);
            prevScore = score;
        }
    }

    @Test
    @Order(3)
    @DisplayName("B: Different queries yield different result sets")
    void differentQueriesYieldDifferentResults() {
        String sig1 = resultsSignature(search("billing invoice payment", 5));
        String sig2 = resultsSignature(search("AML sanctions watchlist", 5));

        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    @Order(4)
    @DisplayName("C: Rebuilding index with same data yields same results")
    void rebuildWithSameDataYieldsSameResults() {
        String query = "onboarding KYC document verification";
        int topK = 5;

        // First search
        String sig1 = resultsSignature(search(query, topK));

        // Rebuild the index with the same data that's already in the search service
        // We do this by re-ingesting (which triggers rebuild)
        restTemplate.postForEntity("/ingest?dry_run=false", null, String.class);

        // Search again after rebuild
        String sig2 = resultsSignature(search(query, topK));

        assertThat(sig1).isEqualTo(sig2);
    }

    @Test
    @Order(5)
    @DisplayName("D: Empty query returns 400")
    void emptyQueryReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/search",
                new HttpEntity<>("{\"query\":\"\",\"top_k\":5}", headers),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(6)
    @DisplayName("E: Search index handles empty state gracefully")
    void emptyIndexReturnsEmptyResults() {
        // Clear index
        searchIndex.rebuild(List.of());
        assertThat(searchIndex.size()).isEqualTo(0);

        JsonNode result = search("fraud AML billing", 5);
        assertThat(result.path("results").size()).isEqualTo(0);

        // Reload the index for subsequent tests
        List<Case> cases = List.of(
            new Case(999L, 1L, "Test fraud case", "Fraud detection test description billing reconciliation", "open",
                     OffsetDateTime.now(ZoneOffset.UTC))
        );
        searchIndex.rebuild(cases);
        assertThat(searchIndex.size()).isEqualTo(1);
    }

    @Test
    @Order(7)
    @DisplayName("F: Results contain required fields: case_id, score, title, status")
    void resultsHaveRequiredFields() {
        JsonNode result = search("payment reconciliation audit", 3);
        JsonNode results = result.path("results");

        for (JsonNode r : results) {
            assertThat(r.has("case_id")).isTrue();
            assertThat(r.has("score")).isTrue();
            assertThat(r.has("title")).isTrue();
            assertThat(r.has("status")).isTrue();
            assertThat(r.path("score").asDouble()).isGreaterThan(0.0);
            assertThat(r.path("title").asText()).isNotBlank();
        }
    }

    @Test
    @Order(8)
    @DisplayName("G: top_k limits the number of results returned")
    void topKLimitsResults() {
        for (int k : List.of(1, 3, 5, 10)) {
            JsonNode result = search("compliance audit fraud billing", k);
            assertThat(result.path("results").size()).isLessThanOrEqualTo(k);
        }
    }

    @Test
    @Order(9)
    @DisplayName("H: Search is deterministic across different JVM-level call orderings")
    void searchIsDeterministicAcrossOrderings() {
        // Run multiple distinct queries, then re-run them in reverse and check same answers
        String[] queries = {
            "AML transaction monitoring",
            "billing invoice dispute",
            "fraud chargeback",
            "onboarding KYC",
            "reconciliation mismatch"
        };

        String[] forward = new String[queries.length];
        for (int i = 0; i < queries.length; i++) {
            forward[i] = resultsSignature(search(queries[i], 5));
        }

        String[] backward = new String[queries.length];
        for (int i = queries.length - 1; i >= 0; i--) {
            backward[i] = resultsSignature(search(queries[i], 5));
        }

        for (int i = 0; i < queries.length; i++) {
            assertThat(backward[i])
                .as("Query '%s' result must be identical regardless of call order", queries[i])
                .isEqualTo(forward[i]);
        }
    }
}
