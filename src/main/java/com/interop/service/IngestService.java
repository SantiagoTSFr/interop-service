package com.interop.service;

import com.interop.model.Case;
import com.interop.model.Customer;
import com.interop.model.IngestManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Core incremental ingest logic.
 *
 * Atomicity contract:
 *   The checkpoint is ONLY advanced after lake writes AND event emission
 *   both succeed. If either fails, the checkpoint remains at its prior value,
 *   making the next run safely retry the same delta.
 *
 *   Lake writes use write-to-temp + atomic rename per partition file,
 *   so partial writes do not leave corrupt data.
 */
@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final JdbcTemplate jdbc;
    private final CheckpointService checkpointService;
    private final LakeWriterService lakeWriter;
    private final EventEmitterService eventEmitter;
    private final SchemaFingerprintService fingerprintService;
    private final SearchIndexService searchIndex;

    public IngestService(JdbcTemplate jdbc,
                         CheckpointService checkpointService,
                         LakeWriterService lakeWriter,
                         EventEmitterService eventEmitter,
                         SchemaFingerprintService fingerprintService,
                         SearchIndexService searchIndex) {
        this.jdbc = jdbc;
        this.checkpointService = checkpointService;
        this.lakeWriter = lakeWriter;
        this.eventEmitter = eventEmitter;
        this.fingerprintService = fingerprintService;
        this.searchIndex = searchIndex;
    }

    /**
     * Run an ingest cycle.
     *
     * @param dryRun if true: compute deltas, return manifest, do NOT write lake/events/checkpoint.
     */
    public IngestManifest ingest(boolean dryRun) throws IOException {
        String runId = UUID.randomUUID().toString();
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);

        // --- 1. Read checkpoint ---
        Optional<OffsetDateTime> checkpointOpt = checkpointService.read();
        OffsetDateTime checkpoint = checkpointOpt.orElse(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        String checkpointBefore = checkpointService.format(checkpointOpt.orElse(null));

        log.info("Ingest run={} dryRun={} checkpoint={}", runId, dryRun, checkpointBefore);

        // --- 2. Fetch deltas ---
        List<Customer> customers = fetchCustomersDelta(checkpoint);
        List<Case> cases = fetchCasesDelta(checkpoint);

        log.info("Delta: {} customers, {} cases", customers.size(), cases.size());

        // --- 3. Compute new watermark ---
        OffsetDateTime newWatermark = computeWatermark(checkpoint, customers, cases);
        String checkpointAfter = checkpointService.format(newWatermark.equals(checkpoint) ? null : newWatermark);
        // If nothing changed, checkpoint_after = checkpoint_before
        if (newWatermark.equals(checkpoint)) {
            checkpointAfter = checkpointBefore;
        }

        // --- 4. Schema fingerprints ---
        String customerFingerprint = fingerprintService.fingerprint("customers");
        String casesFingerprint = fingerprintService.fingerprint("cases");

        // --- 5. DRY RUN path ---
        if (dryRun) {
            OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);
            Map<String, IngestManifest.TableManifest> tables = new LinkedHashMap<>();
            tables.put("customers", new IngestManifest.TableManifest(
                    customers.size(), Collections.emptyList(), customerFingerprint));
            tables.put("cases", new IngestManifest.TableManifest(
                    cases.size(), Collections.emptyList(), casesFingerprint));

            return new IngestManifest(runId, startedAt, finishedAt, true,
                    tables, checkpointBefore, checkpointAfter);
        }

        // --- 6. REAL RUN: write lake (may throw) ---
        List<String> customerPaths = Collections.emptyList();
        List<String> casePaths = Collections.emptyList();

        if (!customers.isEmpty()) {
            customerPaths = lakeWriter.write("customers", customers,
                    c -> c.updatedAt().toLocalDate());
        }
        if (!cases.isEmpty()) {
            casePaths = lakeWriter.write("cases", cases,
                    c -> c.updatedAt().toLocalDate());
        }

        // --- 7. Build manifest ---
        OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, IngestManifest.TableManifest> tables = new LinkedHashMap<>();
        tables.put("customers", new IngestManifest.TableManifest(
                customers.size(), customerPaths, customerFingerprint));
        tables.put("cases", new IngestManifest.TableManifest(
                cases.size(), casePaths, casesFingerprint));

        IngestManifest manifest = new IngestManifest(
                runId, startedAt, finishedAt, false,
                tables, checkpointBefore, checkpointAfter);

        // --- 8. Emit events (may throw) ---
        eventEmitter.emit("customers", manifest);
        eventEmitter.emit("cases", manifest);

        // --- 9. Advance checkpoint ONLY after lake + events succeed ---
        if (!newWatermark.equals(checkpoint)) {
            checkpointService.write(newWatermark);
        }

        // --- 10. Refresh search index with new cases ---
        for (Case c : cases) {
            searchIndex.upsert(c);
        }

        log.info("Ingest complete run={} customers={} cases={}", runId, customers.size(), cases.size());
        return manifest;
    }

    // ---------------------------------------------------------------
    // DB queries
    // ---------------------------------------------------------------

    private List<Customer> fetchCustomersDelta(OffsetDateTime checkpoint) {
        return jdbc.query(
                "SELECT customer_id, name, email, country, updated_at " +
                        "FROM customers WHERE updated_at > ? ORDER BY updated_at ASC",
                (rs, row) -> new Customer(
                        rs.getLong("customer_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("country"),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                checkpoint
        );
    }

    private List<Case> fetchCasesDelta(OffsetDateTime checkpoint) {
        return jdbc.query(
                "SELECT case_id, customer_id, title, description, status, updated_at " +
                        "FROM cases WHERE updated_at > ? ORDER BY updated_at ASC",
                (rs, row) -> new Case(
                        rs.getLong("case_id"),
                        rs.getLong("customer_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                checkpoint
        );
    }

    // ---------------------------------------------------------------
    // Watermark computation
    // ---------------------------------------------------------------

    private OffsetDateTime computeWatermark(OffsetDateTime current,
                                             List<Customer> customers,
                                             List<Case> cases) {
        OffsetDateTime max = current;
        for (Customer c : customers) {
            if (c.updatedAt().isAfter(max)) max = c.updatedAt();
        }
        for (Case c : cases) {
            if (c.updatedAt().isAfter(max)) max = c.updatedAt();
        }
        return max;
    }

    // ---------------------------------------------------------------
    // Startup: load full index from DB
    // ---------------------------------------------------------------

    /**
     * Called on application startup to populate the in-memory search index
     * from the full cases table (reflects already-ingested + any DB data).
     */
    public void loadFullIndex() {
        List<Case> allCases = jdbc.query(
                "SELECT case_id, customer_id, title, description, status, updated_at FROM cases ORDER BY case_id",
                (rs, row) -> new Case(
                        rs.getLong("case_id"),
                        rs.getLong("customer_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getObject("updated_at", OffsetDateTime.class)
                )
        );
        searchIndex.rebuild(allCases);
        log.info("Search index loaded with {} cases on startup", allCases.size());
    }
}
