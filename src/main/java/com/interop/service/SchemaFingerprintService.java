package com.interop.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Computes a deterministic SHA-256 fingerprint of a table's column names and types.
 */
@Service
public class SchemaFingerprintService {

    private final JdbcTemplate jdbcTemplate;

    public SchemaFingerprintService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String fingerprint(String tableName) {
        List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                """
                SELECT column_name, data_type
                FROM information_schema.columns
                WHERE table_name = ?
                ORDER BY ordinal_position
                """,
                tableName
        );

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> col : cols) {
            sb.append(col.get("column_name")).append(":").append(col.get("data_type")).append(";");
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16); // first 16 hex chars
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
