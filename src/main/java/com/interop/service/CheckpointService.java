package com.interop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Manages the checkpoint stored at ./state/checkpoint.json.
 *
 * Atomicity strategy: write to a temp file in the same directory,
 * then atomically rename (which is atomic on POSIX filesystems).
 * The checkpoint is only advanced AFTER lake writes and event emission succeed.
 */
@Service
public class CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointService.class);
    private static final String CHECKPOINT_KEY = "watermark";

    @Value("${interop.state.checkpoint-path}")
    private String checkpointPath;

    private final ObjectMapper objectMapper;

    public CheckpointService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Read current checkpoint. Returns empty if checkpoint file does not exist
     * (first run: ingest everything).
     */
    public Optional<OffsetDateTime> read() {
        Path path = Path.of(checkpointPath);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(path.toFile());
            String ts = node.path(CHECKPOINT_KEY).asText(null);
            if (ts == null || ts.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(OffsetDateTime.parse(ts, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } catch (IOException e) {
            log.warn("Could not read checkpoint, treating as first run: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Atomically persist a new watermark timestamp.
     * Uses write-to-temp + rename strategy for atomicity.
     */
    public void write(OffsetDateTime watermark) throws IOException {
        Path dir = Path.of(checkpointPath).getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }

        ObjectNode node = objectMapper.createObjectNode();
        node.put(CHECKPOINT_KEY, watermark.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        node.put("updated_at", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // Write to temp file, then atomic rename
        Path target = Path.of(checkpointPath);
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), node);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        log.info("Checkpoint advanced to {}", watermark);
    }

    /** Convenience: format checkpoint as ISO string (or "epoch" for null). */
    public String format(OffsetDateTime ts) {
        return ts == null ? "1970-01-01T00:00:00Z" : ts.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
