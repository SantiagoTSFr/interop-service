# AI Usage Log

## Tools / Agents Used

- **Claude Sonnet 4** (via claude.ai) — primary coding assistant for architecture design, code generation, and documentation.
- AI coding tools (GitHub Copilot, Cursor, etc.) were used in this session for some coding standards and completions.

---

## Key Prompts / Transcript Excerpts

### Prompt 1 — Initial architecture scoping
> "I have a requirement for a project with the following criteria that must be completed exactly as it is: [full spec]. The implementation must be done in Java, Spring Boot, Docker and PostgreSQL with all the points completed and verified with the automated tests mentioned."

**What the agent did:** Broke down the spec into layers (DB contract → service → tests → docs), then started generating files in dependency order (docker-compose → init.sql → pom.xml → models → services → controllers → tests).

---

### Prompt 2 — Continue generation
> "Continue generating the next files in the same order as before, starting with the SearchIndexService and working through to the tests and docs. Make sure to follow the spec exactly and maintain consistency with previously generated code."

**What the agent did:** Picked up exactly where it left off (SearchIndexService → IngestService → controllers → AppStartupRunner → JacksonConfig → tests → docs).


### Prompt 3 — Continue generation
> "Please give me an atomic approach to handle FS for checkpoint service."

**What the agent did:** attern used everywhere
write data → temp file (.tmp)  →  Files.move(tmp, target, ATOMIC_MOVE + REPLACE_EXISTING)
Applied to three places
1. Checkpoint (checkpoint.json)

    ```
    Path tmp = target.resolveSibling("checkpoint.json.tmp");
    objectMapper.writeValue(tmp.toFile(), node);          // write to .tmp
    Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING); // rename atomically
2. Then I've implemented the same pattern for the JSONL files in LakeWriterService to ensure idempotent overwrites on reruns.
---

## What Was Verified Manually

### Schema and seed data
- Counted INSERT rows in `init.sql` to confirm exactly 30 customers and 200 cases → 200 ✓
- Verified all required columns match the DDL contract by reading through the CREATE TABLE statements side-by-side with the spec.
- Confirmed `updated_at` spans 30 days via `now() - interval '30 days'` through `now() - interval '1 minute'` patterns.

### Checkpoint atomicity
- Reviewed `CheckpointService.write()` — uses `Files.move(tmp, target, ATOMIC_MOVE)` which is POSIX-atomic.
- Reviewed `IngestService.ingest()` — the checkpoint write call is the **last** operation after lake writes and event emission both complete without exception. Any IOException before that point leaves the checkpoint untouched.

### JSONL idempotency
- Reviewed `LakeWriterService.write()` — writes to `.tmp` then `ATOMIC_MOVE` to final path with `REPLACE_EXISTING`. Re-running with same data overwrites, not appends → no duplicates.

### Search determinism
- Traced the full code path: `tokenise()` → `termVector()` → `murmurMix()` → `l2Normalise()` → `cosineSimilarity()` → sort by score DESC then case_id ASC.
- Confirmed no use of `Math.random()`, `UUID.randomUUID()`, `HashMap` (non-deterministic iteration order), or time-based values anywhere in the scoring pipeline. All uses of `LinkedHashMap` for order stability.
- The tie-break sort on `case_id` ensures fully deterministic ordering even when scores are equal.

### Test completeness
- Confirmed that `BaseIntegrationTest` uses `@DynamicPropertySource` to wire Testcontainers JDBC URL into Spring context — no hardcoded localhost connection in tests.
- Confirmed `db/init.sql` is copied to `src/test/resources/db/init.sql` so Testcontainers `withInitScript("db/init.sql")` can find it on the classpath.

### Changes script
- Reviewed `db/changes.sql` — `UPDATE` targets 5 specific case_ids, `INSERT` for 2 customers uses fixed email/name strings (fully deterministic), 10 new cases reference customer_ids 1–32.
- Note: the 2 new customers get IDs 31 and 32 because the identity sequence starts at 1 and 30 customers were seeded. The changes.sql references `customer_id IN (31, 32)` which matches this sequence.

---

## What the Agent Got Wrong / Corrections Made

### Issue 1: Case generated non-deterministically in `init.sql`
```
-- db/init.sql

-- Insert customers (30)
INSERT INTO customers (name, email, country, updated_at)
SELECT
    'Customer ' || i,
    'customer' || i || '@example.com',
    CASE 
        WHEN i % 5 = 0 THEN 'US'
        WHEN i % 5 = 1 THEN 'UK'
        WHEN i % 5 = 2 THEN 'DE'
        WHEN i % 5 = 3 THEN 'FR'
        ELSE 'ES'
    END,
    now() - (i || ' days')::interval
FROM generate_series(1, 30) AS s(i);

-- Insert cases (200)
INSERT INTO cases (customer_id, title, description, status, updated_at)
SELECT
    ((i - 1) % 30) + 1,
    'Case #' || i || ' - ' ||
        CASE 
            WHEN i % 6 = 0 THEN 'Billing Issue'
            WHEN i % 6 = 1 THEN 'Audit Request'
            WHEN i % 6 = 2 THEN 'Compliance Check'
            WHEN i % 6 = 3 THEN 'Payment Failure'
            WHEN i % 6 = 4 THEN 'Fraud Alert'
            ELSE 'Onboarding Help'
        END,
    'Detailed description for case #' || i || 
    ' involving ' ||
        CASE 
            WHEN i % 7 = 0 THEN 'billing discrepancy'
            WHEN i % 7 = 1 THEN 'audit logs'
            WHEN i % 7 = 2 THEN 'compliance validation'
            WHEN i % 7 = 3 THEN 'payment processing'
            WHEN i % 7 = 4 THEN 'reconciliation mismatch'
            WHEN i % 7 = 5 THEN 'fraud detection'
            ELSE 'AML verification'
        END,
    CASE 
        WHEN i % 3 = 0 THEN 'open'
        WHEN i % 3 = 1 THEN 'in_progress'
        ELSE 'closed'
    END,
    now() - ((i % 30) || ' days')::interval
FROM generate_series(1, 200) AS s(i);
```
The first pass of `init.sql` generated a script using conditions like `WHEN i % 5 = 0 THEN 'US'` for country, `WHEN i % 6 = 0 THEN 'Billing Issue'` for title, etc. This is deterministic in theory but the agent's implementation of `generate_series` and modulo logic produced different outputs across runs (likely due to differences in how the AI handles loops and state). The fix was to ensure that the generation logic is consistent and does not rely on any internal state that could vary between runs. ✓ Fixed.

### Issue 2: `HashMap` vs `LinkedHashMap` in term vectors
Initial draft of `SearchIndexService` used `HashMap` for `termVector()` return type, which could cause non-deterministic iteration order during scoring. Corrected to `LinkedHashMap` throughout to maintain insertion order. ✓ Fixed.

### Issue 3: Test resource path for Testcontainers
`withInitScript("db/init.sql")` looks for the file on the classpath, not relative to project root. The init.sql was only in `db/` at project root. Fixed by copying it to `src/test/resources/db/init.sql` in the same generation session. ✓ Fixed.

### Issue 4: `Case.java` Case model generation was inaccurate at the beginning.
The agent named the class in some parts of the code CaseEntity and in some other parts Case, the agent also generated a Java legacy class, then I changed the approach to records. ✓ Fixed to `Case.java` moved to records (java 21).


## Agent Limitations Acknowledged

- The agent cannot run the code — all validation was logic-tracing, code review, and structural analysis rather than live execution.
- Testcontainers pulls `postgres:16` at test time — first run requires internet access and may be slow.
- The search algorithm is intentionally simple (hash-weighted TF cosine). In production this would be replaced by a real embedding model (as documented in ARCHITECTURE_AWS.md).
