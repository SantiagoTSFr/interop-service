package com.interop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes JSONL files to the local lake directory.
 *
 * Layout: ./lake/{table}/date=YYYY-MM-DD/data.jsonl
 *
 * Re-running with same data is safe: files are overwritten (not appended).
 * Data is grouped by the date partition derived from updated_at.
 */
@Service
public class LakeWriterService {

    private static final Logger log = LoggerFactory.getLogger(LakeWriterService.class);

    @Value("${interop.lake.base-path}")
    private String lakeBasePath;

    private final ObjectMapper objectMapper;

    public LakeWriterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Write a list of records to the lake, partitioned by date.
     * Returns the list of lake paths written.
     *
     * @param tableName  e.g. "customers" or "cases"
     * @param records    list of objects to serialise as JSONL
     * @param dateExtractor function to extract LocalDate from each record
     */
    public <T> List<String> write(String tableName, List<T> records,
                                   java.util.function.Function<T, LocalDate> dateExtractor)
            throws IOException {

        // Group records by date partition
        Map<LocalDate, List<T>> byDate = new LinkedHashMap<>();
        for (T record : records) {
            LocalDate date = dateExtractor.apply(record);
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
        }

        List<String> paths = new ArrayList<>();

        for (Map.Entry<LocalDate, List<T>> entry : byDate.entrySet()) {
            String dateStr = entry.getKey().toString(); // YYYY-MM-DD
            String relativePath = tableName + "/date=" + dateStr + "/data.jsonl";
            Path filePath = Path.of(lakeBasePath, tableName, "date=" + dateStr, "data.jsonl");
            Files.createDirectories(filePath.getParent());

            // Build JSONL content (overwrite — idempotent)
            StringBuilder sb = new StringBuilder();
            for (T record : entry.getValue()) {
                sb.append(objectMapper.writeValueAsString(record)).append("\n");
            }

            // Atomic write: write to temp then rename
            Path tmp = filePath.resolveSibling("data.jsonl.tmp");
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            paths.add(relativePath);
            log.info("Wrote {} rows to lake path: {}", entry.getValue().size(), relativePath);
        }

        return paths;
    }
}
