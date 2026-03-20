# Review Checklist

## Database Contract
- [x] `docker-compose.yml` uses `postgres:16`, port `5432`, credentials `interop/interop/interop`
- [x] `db/init.sql` creates `customers` and `cases` tables with exact column names and types from spec
- [x] All 3 required indexes present (`customers(updated_at)`, `cases(updated_at)`, `cases(customer_id)`)
- [x] 30 customers seeded (deterministic, no randomness)
- [x] 200 cases seeded (deterministic, no randomness)
- [x] `updated_at` spans last 30 days using `now() - interval` patterns
- [x] Case text includes diverse keywords: billing, audit, compliance, payments, reconciliation, onboarding, fraud, AML, sanctions, KYC, chargeback, GDPR, SOC2
- [x] `db/changes.sql` updates 5 cases (status + updated_at), inserts 2 customers, inserts 10 cases — all deterministic

## Service: /ingest
- [x] Endpoint: `POST /ingest?dry_run=true|false`
- [x] Checkpoint stored at `./state/checkpoint.json`
- [x] Incremental: fetches rows where `updated_at > checkpoint` (strict greater-than)
- [x] `dry_run=true`: computes deltas, returns manifest, NO lake writes, NO events, NO checkpoint update
- [x] `dry_run=false`: writes lake, emits events, returns manifest, advances checkpoint only on full success
- [x] Checkpoint atomicity: write-to-temp + `ATOMIC_MOVE`; checkpoint is last operation
- [x] Failure safety: any IOException leaves checkpoint unchanged

## Service: Lake Output
- [x] JSONL files written to `./lake/{table}/date=YYYY-MM-DD/data.jsonl`
- [x] Valid JSONL (1 JSON object per line)
- [x] Re-run with no DB changes: overwrite (not append) → no duplicates
- [x] Atomic partition writes: tmp file → `ATOMIC_MOVE`

## Service: Manifest
- [x] `run_id` (UUID string)
- [x] `started_at`, `finished_at` (ISO-8601 timestamps)
- [x] Per-table: `delta_row_count`, `lake_paths`, `schema_fingerprint`
- [x] `checkpoint_before`, `checkpoint_after`
- [x] `dry_run` flag

## Service: Delta Events
- [x] One event per table emitted to stdout after successful ingest
- [x] Event fields: `table`, `run_id`, `schema_fingerprint`, `delta_row_count`, `lake_paths`, `checkpoint_after`
- [x] Stretch: events also appended to `./events/events.jsonl`

## Service: /search
- [x] Endpoint: `POST /search` with body `{"query": "text", "top_k": N}`
- [x] Deterministic algorithm (hash-based, no randomness)
- [x] In-memory index loaded from DB on startup; refreshed after each ingest
- [x] Results include `case_id`, `score`, `title`, `status`
- [x] Empty query returns 400

## Tests
- [x] `CheckpointIdempotencyTest`: checkpoint advances only on success; rerun with no DB changes yields `delta_row_count=0` and no duplicates
- [x] `SearchDeterminismTest`: same query + same data ⇒ same ordered top_k results
- [x] Tests use Testcontainers (real Postgres 16, no mocks for DB)
- [x] Tests are fully automated (`mvn test`)

## Documentation
- [x] `README.md`: prereqs, run instructions, test instructions, example curl calls, assumptions
- [x] `AI_USAGE.md`: tools used, key prompts, manual verification steps, agent corrections
- [x] `ARCHITECTURE_AWS.md`: diagram, service choices, NFRs addressed, cost drivers, tradeoffs (≤1 page)
- [x] `EXECUTION_PLAN.md`: phase breakdown, key decisions, risks (≤1 page)
- [x] `REVIEW_CHECKLIST.md`: this file
