package com.interop.controller;

import com.interop.model.IngestManifest;
import com.interop.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    /**
     * POST /ingest?dry_run=true|false
     * dry_run=true  → compute deltas, return manifest; NO lake writes, NO events, NO checkpoint update
     * dry_run=false → full ingest: lake writes + events + checkpoint advance
     */
    @PostMapping("/ingest")
    public ResponseEntity<IngestManifest> ingest(
            @RequestParam(name = "dry_run", defaultValue = "false") boolean dryRun) {

        log.info("POST /ingest dry_run={}", dryRun);
        try {
            IngestManifest manifest = ingestService.ingest(dryRun);
            return ResponseEntity.ok(manifest);
        } catch (IOException e) {
            log.error("Ingest failed: {}", e.getMessage(), e);
            // Return 500; checkpoint is NOT advanced (atomicity guarantee)
            return ResponseEntity.internalServerError().build();
        }
    }
}
