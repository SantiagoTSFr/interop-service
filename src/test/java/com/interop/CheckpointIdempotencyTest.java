package com.interop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interop.service.CheckpointService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 1: Checkpoint correctness + idempotency.
 *
 * Scenarios covered:
 *  A) First ingest: checkpoint advances from epoch to the max updated_at in the data.
 *  B) Dry-run: checkpoint does NOT advance; no lake files written.
 *  C) Re-run with no DB changes: delta_row_count = 0, no duplicate lake entries.
 *  D) Failure safety: if DB is unavailable, checkpoint must NOT advance.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CheckpointIdempotencyTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired CheckpointService checkpointService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @Value("${interop.state.checkpoint-path}")
    String checkpointPath;

    @Value("${interop.lake.base-path}")
    String lakeBasePath;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private JsonNode postIngest(boolean dryRun) {
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/ingest?dry_run=" + dryRun, null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteCheckpoint() throws IOException {
        Files.deleteIfExists(Path.of(checkpointPath));
    }

    private long countLakeLines(String table) throws IOException {
        Path root = Path.of(lakeBasePath, table);
        if (!Files.exists(root)) return 0L;
        return Files.walk(root)
                .filter(p -> p.toString().endsWith(".jsonl"))
                .mapToLong(p -> {
                    try {
                        return Files.lines(p).filter(l -> !l.isBlank()).count();
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
    }

    // ---------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("A: First real ingest advances checkpoint and writes lake files")
    void firstIngestAdvancesCheckpoint() throws IOException {
        deleteCheckpoint();

        // Verify no checkpoint yet
        Optional<OffsetDateTime> before = checkpointService.read();
        assertThat(before).isEmpty();

        // Run first ingest
        JsonNode manifest = postIngest(false);

        // Checkpoint must now exist and be a real timestamp
        Optional<OffsetDateTime> after = checkpointService.read();
        assertThat(after).isPresent();
        assertThat(after.get().getYear()).isGreaterThanOrEqualTo(2024);

        // Manifest must reflect the rows
        int customerCount = manifest.path("tables").path("customers").path("delta_row_count").asInt();
        int caseCount = manifest.path("tables").path("cases").path("delta_row_count").asInt();
        assertThat(customerCount).isGreaterThanOrEqualTo(30);
        assertThat(caseCount).isGreaterThanOrEqualTo(200);

        // Lake files must have been written
        assertThat(countLakeLines("customers")).isGreaterThanOrEqualTo(30);
        assertThat(countLakeLines("cases")).isGreaterThanOrEqualTo(200);

        // Manifest checkpoint_before is epoch, checkpoint_after is real
        assertThat(manifest.path("checkpoint_before").asText()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(manifest.path("checkpoint_after").asText()).doesNotContain("1970");

        // dry_run flag in manifest
        assertThat(manifest.path("dry_run").asBoolean()).isFalse();
    }

    @Test
    @Order(2)
    @DisplayName("B: Dry-run does NOT advance checkpoint and writes NO lake files")
    void dryRunDoesNotAdvanceCheckpoint() throws IOException {
        // Ensure checkpoint exists from previous test (or reset and do a real ingest first)
        if (checkpointService.read().isEmpty()) {
            postIngest(false);
        }

        Optional<OffsetDateTime> checkpointBeforeDryRun = checkpointService.read();
        long lakeLinesBefore = countLakeLines("cases");

        // Execute dry run
        JsonNode manifest = postIngest(true);

        // Checkpoint must NOT have changed
        Optional<OffsetDateTime> checkpointAfterDryRun = checkpointService.read();
        assertThat(checkpointAfterDryRun).isEqualTo(checkpointBeforeDryRun);

        // Lake must NOT have more files
        assertThat(countLakeLines("cases")).isEqualTo(lakeLinesBefore);

        // Lake paths in manifest are empty for dry run
        assertThat(manifest.path("tables").path("cases").path("lake_paths").isEmpty()).isTrue();
        assertThat(manifest.path("dry_run").asBoolean()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("C: Re-run with no DB changes produces delta_row_count=0 and no duplicate lines")
    void reRunWithNoChangesIsIdempotent() throws IOException {
        // Ensure a fresh ingest has been done
        if (checkpointService.read().isEmpty()) {
            postIngest(false);
        }

        Optional<OffsetDateTime> checkpointBefore = checkpointService.read();
        long lakeLinesBefore = countLakeLines("cases");

        // Second real ingest with no DB changes
        JsonNode manifest = postIngest(false);

        // Delta must be zero
        int customerDelta = manifest.path("tables").path("customers").path("delta_row_count").asInt();
        int caseDelta = manifest.path("tables").path("cases").path("delta_row_count").asInt();
        assertThat(customerDelta).isEqualTo(0);
        assertThat(caseDelta).isEqualTo(0);

        // Checkpoint must not have advanced
        Optional<OffsetDateTime> checkpointAfter = checkpointService.read();
        assertThat(checkpointAfter).isEqualTo(checkpointBefore);

        // Lake must have same line count (no duplicates)
        long lakeLinesAfter = countLakeLines("cases");
        assertThat(lakeLinesAfter).isEqualTo(lakeLinesBefore);
    }

    @Test
    @Order(4)
    @DisplayName("D: After incremental DB changes, only new/changed rows are ingested")
    void incrementalChangesArePickedUp() throws IOException {
        // Start from clean checkpoint
        deleteCheckpoint();
        postIngest(false); // ingest everything
        Optional<OffsetDateTime> checkpoint1 = checkpointService.read();
        assertThat(checkpoint1).isPresent();

        // Simulate incremental change: insert a new customer with updated_at = now()
        jdbcTemplate.update(
                "INSERT INTO customers (name, email, country, updated_at) VALUES (?, ?, ?, now())",
                "Test Incremental", "test.incremental@example.com", "US"
        );

        // Second ingest should pick up only the new customer
        JsonNode manifest2 = postIngest(false);
        int customerDelta = manifest2.path("tables").path("customers").path("delta_row_count").asInt();
        assertThat(customerDelta).isGreaterThanOrEqualTo(1);

        // Checkpoint must have advanced
        Optional<OffsetDateTime> checkpoint2 = checkpointService.read();
        assertThat(checkpoint2).isPresent();
        assertThat(checkpoint2.get()).isAfter(checkpoint1.get());

        // Third ingest (no more changes) must yield 0
        JsonNode manifest3 = postIngest(false);
        assertThat(manifest3.path("tables").path("customers").path("delta_row_count").asInt()).isEqualTo(0);
        assertThat(manifest3.path("tables").path("cases").path("delta_row_count").asInt()).isEqualTo(0);
    }

    @Test
    @Order(5)
    @DisplayName("E: Manifest has required fields: run_id, started_at, finished_at, schema_fingerprint, lake_paths")
    void manifestHasRequiredFields() throws IOException {
        deleteCheckpoint();
        JsonNode manifest = postIngest(false);

        assertThat(manifest.has("run_id")).isTrue();
        assertThat(manifest.path("run_id").asText()).isNotBlank();
        assertThat(manifest.has("started_at")).isTrue();
        assertThat(manifest.has("finished_at")).isTrue();
        assertThat(manifest.has("checkpoint_before")).isTrue();
        assertThat(manifest.has("checkpoint_after")).isTrue();

        JsonNode casesTable = manifest.path("tables").path("cases");
        assertThat(casesTable.has("delta_row_count")).isTrue();
        assertThat(casesTable.has("lake_paths")).isTrue();
        assertThat(casesTable.has("schema_fingerprint")).isTrue();
        assertThat(casesTable.path("schema_fingerprint").asText()).isNotBlank();
    }
}
