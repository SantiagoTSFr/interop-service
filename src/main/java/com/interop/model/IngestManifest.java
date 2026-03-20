package com.interop.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record IngestManifest(
        @JsonProperty("run_id") String runId,
        @JsonProperty("started_at") OffsetDateTime startedAt,
        @JsonProperty("finished_at") OffsetDateTime finishedAt,
        @JsonProperty("dry_run") boolean dryRun,
        @JsonProperty("tables") Map<String, TableManifest> tables,
        @JsonProperty("checkpoint_before") String checkpointBefore,
        @JsonProperty("checkpoint_after") String checkpointAfter
) {
    public record TableManifest(
            @JsonProperty("delta_row_count") int deltaRowCount,
            @JsonProperty("lake_paths") List<String> lakePaths,
            @JsonProperty("schema_fingerprint") String schemaFingerprint
    ) {}
}
