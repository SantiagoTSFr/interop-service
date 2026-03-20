# Execution Plan

## Approach

Strict dependency-first order: DB contract → core models → stateful services → HTTP layer → tests → docs. Each layer is independently testable before the next is built.

## Phase Breakdown

| Phase | What | Time Estimate |
|-------|------|---------------|
| 1 | DB contract: `docker-compose.yml`, `db/init.sql` (schema + 30 customers + 200 cases), `db/changes.sql` | 20 min |
| 2 | Spring Boot skeleton: `pom.xml`, `application.properties`, main class, `JacksonConfig` | 10 min |
| 3 | Core models: `Customer`, `Case`, `IngestManifest`, `SearchModels` | 10 min |
| 4 | Stateful services: `CheckpointService`, `LakeWriterService`, `EventEmitterService`, `SchemaFingerprintService` | 25 min |
| 5 | Business logic: `IngestService` (delta fetch, watermark, atomicity), `SearchIndexService` (hash-TF-cosine) | 25 min |
| 6 | HTTP layer: `IngestController`, `SearchController`, `AppStartupRunner` | 10 min |
| 7 | Tests: `BaseIntegrationTest` (Testcontainers), `CheckpointIdempotencyTest`, `SearchDeterminismTest` | 20 min |
| 8 | Docs: `README.md`, `AI_USAGE.md`, `ARCHITECTURE_AWS.md`, `EXECUTION_PLAN.md`, `REVIEW_CHECKLIST.md` | 20 min |

**Total: ~2 hours**

## Key Design Decisions Made During Execution

### Atomicity implementation
Chose write-to-temp + `Files.move(ATOMIC_MOVE)` on local FS (no distributed tx needed for local). Checkpoint is the last write in the happy path — any prior failure leaves it untouched, making reruns safe.

### Search algorithm
Chose hash-weighted TF cosine similarity over BM25 or neural embeddings. Rationale: fully deterministic with no external dependencies; understandable and auditable; fast enough for the test scope. The architecture doc covers the production-grade replacement (Bedrock + OpenSearch).

### Test strategy
Integration tests over unit tests for the critical paths (checkpoint + search), because the correctness guarantees are end-to-end: a unit-tested service with a mocked DB cannot prove that the checkpoint advances correctly relative to real TIMESTAMPTZ semantics in Postgres. Testcontainers provides a real Postgres 16 instance in CI with zero infrastructure setup.

### JSONL partitioning
Partitioned by `updated_at` date. Chose overwrite-not-append semantics per partition file to ensure idempotency on reruns. Files contain all rows for that date from the current delta, so a partial rerun of the same day replaces the file rather than duplicating rows.

## Risks Accepted

- Testcontainers pulls `postgres:16` on first test run — requires internet access.
- Local FS atomicity relies on POSIX `rename()` semantics — not guaranteed on all Windows filesystems (not a concern for Docker/Linux).
- Search index is in-memory: service restart requires rebuild from DB (fast, ~seconds for 200 cases; documented in README).
