# AWS Architecture (Design Doc — No Implementation Required)

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│  SOURCE                                                                 │
│  Aurora PostgreSQL 16 (Multi-AZ, 50M customers / 200M cases)            │
│  ↓ logical replication / CDC via AWS DMS                                │
└───────────────────────────┬─────────────────────────────────────────────┘
                            │ Change stream (per-tenant partition key)
                            ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  INGEST LAYER                                                           │
│  Amazon MSK (Kafka) — topics: customers-delta, cases-delta              │
│  • Partitioned by tenant_id (5,000 partitions)                          │
│  • Retention 7 days (replay/RPO buffer)                                 │
└───────┬───────────────────────────────────────────────────────┬─────────┘
        │                                                       │
        ▼                                                       ▼
┌────────────────────┐                              ┌──────────────────────┐
│  LAKE WRITER       │                              │  SEARCH INDEXER      │
│  ECS tasks         │                              │  ECS tasks           │
│  • Consume Kafka   │                              │  • Consume Kafka     │
│  • Write JSONL     │                              │  • Embed via         │
│    to S3 (lake)    │                              │    Bedrock Titan     │
│  • Partitioned by  │                              │  • Upsert to         │
│    tenant+date     │                              │    OpenSearch        │
│  • Checkpoint in   │                              │  • k-NN index        │
│    DynamoDB        │                              └──────────────────────┘
└────────────────────┘                                        │
        │                                                     │
        ▼                                                     ▼
┌────────────────────┐                              ┌──────────────────────┐
│  S3 DATA LAKE      │                              │  OpenSearch          │
│  s3://lake/        │                              │  Serverless          │
│  {tenant}/         │                              │  • Tenant-isolated   │
│  customers/        │                              │    indices           │
│  date=YYYY-MM-DD/  │                              │  • k-NN vector       │
│  data.jsonl.gz     │                              │    search            │
│  • Versioning ON   │                              │  • 300 QPS steady    │
│  • S3 Object Lock  │                              │  • p95 < 300ms       │
│    (WORM, 7yr)     │                              └──────────────────────┘
└────────────────────┘

  APIs: API Gateway → ECS (Spring Boot) → Aurora (read replica) / OpenSearch
  Events: SNS fan-out after successful ingest batch → downstream consumers
```

## Service Choices & Rationale

| Layer               | Choice | Reason |
|---------------------|--------|--------|
| DB                  | Aurora PostgreSQL 16 Multi-AZ | Logical replication for CDC; auto-failover (RTO < 30 min) |
| Change data capture | AWS DMS + MSK | Managed, exactly-once delivery; 5K updates/sec throughput |
| Lake storage        | S3 + Object Lock | Effectively zero data loss; WORM for compliance; versioning for replay |
| Checkpoints         | DynamoDB (conditional writes) | Atomic compare-and-swap per consumer group — prevents dual-write |
| Embeddings          | Amazon Bedrock Titan | Managed, no GPU infra; GDPR-compliant within AWS region |
| Search              | OpenSearch Serverless | Auto-scales to 1,000 QPS burst; k-NN native; tenant index isolation |
| Compute             | ECS Fargate | No server management; scales to 5K tenants; IAM task roles (least privilege) |
| API                 | API Gateway + ECS | WAF, throttling, mTLS termination; 99.9% SLA covered by managed services |

## Freshness SLA: 10-Minute p95

Change data capture → MSK → Lake Writer → S3 target: **~2–4 min** at peak throughput.  
Change data capture → MSK → Indexer → OpenSearch target: **~3–6 min** including embedding.  
Both are well within the 10-minute p95 SLA with headroom for retries.

## NFRs: How They Are Met

- **Reliability 99.9%**: Multi-AZ Aurora + Fargate multi-AZ; API Gateway SLA 99.95%; DynamoDB SLA 99.999%.
- **RPO ≤ 5 min**: MSK 7-day retention as replay buffer; DMS replicates changes in near-real-time.
- **RTO ≤ 30 min**: Aurora failover ~60s; ECS tasks restart in <5 min; checkpoint replay from MSK offset.
- **Lake durability (zero loss)**: S3 11-nines durability + Object Lock (WORM) + Cross-Region Replication.
- **Security/PII**: KMS encryption at rest and in transit; VPC isolation; IAM least-privilege task roles; CloudTrail audit logging; GDPR deletion via S3 Lifecycle + OpenSearch delete-by-query.
- **Search p95 < 300ms**: OpenSearch Serverless k-NN with HNSW graph; response cached at API Gateway for repeated queries.

## Top 3 Cost Drivers & Controls

1. **OpenSearch Serverless OCU** — largest spend at scale. Control: right-size OCU reservations; use index lifecycle to archive cold tenant data to S3; cache hot queries at API Gateway (TTL 60s).
2. **Bedrock Titan embedding calls** — charged per token at 200M cases. Control: batch embed on ingest (not at query time); cache embeddings in S3 keyed by content hash; skip re-embedding unchanged rows.
3. **MSK + DMS data transfer** — inter-AZ traffic costs at 5K updates/sec. Control: use MSK tiered storage for older offsets; compress Kafka messages (LZ4); place DMS replication instance in same AZ as Aurora primary.

## Tradeoffs & Assumptions

- **Multi-tenant isolation**: Index-per-tenant in OpenSearch provides hard data isolation but increases operational overhead vs. field-based filtering. Acceptable for SOC2/GDPR compliance requirements.
- **Eventual consistency**: Lake writes and search index are asynchronous to the source DB. Reads within the 10-minute SLA window may see slightly stale data — acceptable per spec.
- **Embedding model lock-in**: Bedrock Titan chosen for managed infra; migrating to a different model would require re-indexing all 200M cases.
- **Checkpoint in DynamoDB**: Conditional writes (`attribute_not_exists` + version check) provide optimistic locking. If a Fargate task crashes mid-write, the next task replays from the last committed offset — safe but may produce duplicate lake partitions (overwrite is idempotent).
- **GDPR deletion**: Right-to-erasure is handled via S3 Object Tagging + Lifecycle rules (delete after retention period) and OpenSearch delete-by-query on `customer_id`. Aurora point-in-time backups are excluded from erasure scope per regulator guidance for audit trail integrity.
