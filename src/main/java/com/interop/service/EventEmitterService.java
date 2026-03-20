package com.interop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interop.model.IngestManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emits one JSON event per table to stdout after a successful ingest.
 * Also appends events to ./events/events.jsonl (stretch goal).
 */
@Service
public class EventEmitterService {

    private static final Logger log = LoggerFactory.getLogger(EventEmitterService.class);

    @Value("${interop.events.path}")
    private String eventsPath;

    private final ObjectMapper objectMapper;

    public EventEmitterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void emit(String tableName, IngestManifest manifest) throws IOException {
        IngestManifest.TableManifest tm = manifest.tables().get(tableName);
        if (tm == null) return;

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("table", tableName);
        event.put("run_id", manifest.runId());
        event.put("schema_fingerprint", tm.schemaFingerprint());
        event.put("delta_row_count", tm.deltaRowCount());
        event.put("lake_paths", tm.lakePaths());
        event.put("checkpoint_after", manifest.checkpointAfter());

        String json = objectMapper.writeValueAsString(event);

        // Emit to stdout (required)
        System.out.println("[EVENT] " + json);

        // Append to events.jsonl (stretch)
        appendToEventsFile(json);
    }

    private void appendToEventsFile(String json) {
        try {
            Path path = Path.of(eventsPath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, json + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Failed to append event to {}: {}", eventsPath, e.getMessage());
        }
    }
}
