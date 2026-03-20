# Interop Service

Mini Data Interop + Event Sync + AI-Ready Search — built with Java 21, Spring Boot 3, PostgreSQL 16.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Docker + Docker Compose | 24+ |
| Java | 21 |
| Maven | 3.9+ (or use `./mvnw`) |

---

## Quick Start

### 1. Start PostgreSQL

```bash
docker compose up -d
# Wait for healthy status
docker compose ps
```

The database is initialised automatically from `db/init.sql` (30 customers, 200 cases).

### 2. Start the service

```bash
mvn spring-boot:run
# Service starts on http://localhost:8080
```

Or build and run the JAR:

```bash
mvn package -DskipTests
java -jar target/interop-service-1.0.0.jar
```

---

## Running Tests

Tests use **Testcontainers** — they spin up their own Postgres 16 container automatically. Docker must be running.

```bash
mvn test
```

Two test classes, 13 scenarios total:
- `CheckpointIdempotencyTest` — checkpoint correctness and idempotency (5 scenarios)
- `SearchDeterminismTest` — search determinism (8 scenarios)

---

## API Examples

### POST /ingest

**Dry run** — compute deltas, no writes:
```bash
curl -s -X POST "http://localhost:8080/ingest?dry_run=true" | jq .
```

**Real ingest** — write lake, emit events, advance checkpoint:
```bash
curl -s -X POST "http://localhost:8080/ingest?dry_run=false" | jq .
```

Example response:
```json
{
  "run_id": "a1b2c3d4-...",
  "started_at": "2025-01-15T10:00:00Z",
  "finished_at": "2025-01-15T10:00:01.234Z",
  "dry_run": false,
  "tables": {
    "customers": {
      "delta_row_count": 30,
      "lake_paths": ["customers/date=2025-01-15/data.jsonl"],
      "schema_fingerprint": "a1b2c3d4e5f6a7b8"
    },
    "cases": {
      "delta_row_count": 200,
      "lake_paths": ["cases/date=2025-01-15/data.jsonl", "..."],
      "schema_fingerprint": "b2c3d4e5f6a7b8c9"
    }
  },
  "checkpoint_before": "1970-01-01T00:00:00Z",
  "checkpoint_after": "2025-01-15T09:59:58.123Z"
}
```

### Apply incremental changes between two ingests

```bash
# Run first ingest
curl -s -X POST "http://localhost:8080/ingest?dry_run=false" | jq .tables

# Apply DB changes
PGPASSWORD=interop psql -h localhost -U interop -d interop -f db/changes.sql

# Run second ingest — only the changed rows appear in delta
curl -s -X POST "http://localhost:8080/ingest?dry_run=false" | jq .tables
```

### POST /search

```bash
curl -s -X POST http://localhost:8080/search \
  -H "Content-Type: application/json" \
  -d '{"query": "AML compliance fraud transaction", "top_k": 5}' | jq .
```

Example response:
```json
{
  "query": "AML compliance fraud transaction",
  "top_k": 5,
  "results": [
    {"case_id": 3,  "score": 0.412381, "title": "AML alert triggered on account", "status": "open"},
    {"case_id": 18, "score": 0.389102, "title": "AML transaction monitoring escalation", "status": "in_progress"},
    {"case_id": 44, "score": 0.371445, "title": "Fraud investigation - account takeover", "status": "open"},
    {"case_id": 56, "score": 0.345821, "title": "AML periodic review due", "status": "in_progress"},
    {"case_id": 79, "score": 0.312019, "title": "Fraud ring detection alert", "status": "open"}
  ]
}
```

---

## File Outputs

| Path | Description |
|------|-------------|
| `./state/checkpoint.json` | Current watermark timestamp |
| `./lake/customers/date=YYYY-MM-DD/data.jsonl` | Partitioned customer JSONL |
| `./lake/cases/date=YYYY-MM-DD/data.jsonl` | Partitioned cases JSONL |
| `./events/events.jsonl` | Appended delta events (stretch) |

---

## Assumptions

- **Checkpoint epoch**: On first run (no checkpoint file), all rows are ingested from the beginning of time.
- **Lake partitioning**: Rows are partitioned by `updated_at` date. If the same date partition is written twice, the file is overwritten (not appended), preventing duplicates.
- **Search index**: Loaded from the full DB on startup. Refreshed incrementally after each successful ingest.
- **Atomicity**: Lake writes use write-to-temp + `Files.move(ATOMIC_MOVE)`. Checkpoint is only advanced after lake writes and event emission both succeed.
- **JSONL validity**: Each line is a complete, self-contained JSON object.
- **Port**: The service defaults to port `8080`. Override with `--server.port=XXXX`.
