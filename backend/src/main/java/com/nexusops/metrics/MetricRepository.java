package com.nexusops.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class MetricRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MetricRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void insertBatch(UUID serverId, java.util.List<MetricSample> samples) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO metric_samples
                    (server_id, metric_type, metric_name, value, unit, labels, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
                """, samples, samples.size(), (ps, sample) -> {
            ps.setObject(1, serverId);
            ps.setString(2, sample.metricType());
            ps.setString(3, sample.metricName());
            ps.setBigDecimal(4, sample.value());
            ps.setString(5, sample.unit());
            try {
                ps.setString(6, objectMapper.writeValueAsString(sample.labels()));
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Metric labels must be valid JSON", exception);
            }
            Instant recordedAt = sample.recordedAt();
            ps.setTimestamp(7, Timestamp.from(recordedAt == null ? Instant.now() : recordedAt));
        });
    }

    public List<MetricSample> findLatestForServer(UUID serverId, int limit) {
        return jdbcTemplate.query("""
            SELECT metric_type, metric_name, value, unit, labels_json, recorded_at
            FROM (
                SELECT metric_type, metric_name, value, unit, labels::text AS labels_json, recorded_at, id
                FROM metric_samples
                WHERE server_id = ?
                ORDER BY recorded_at DESC, id DESC
                LIMIT ?
            ) recent
            ORDER BY recorded_at ASC, id ASC
            """, (rs, rowNum) -> {
            Map<String, Object> labels;
            try {
            labels = objectMapper.readValue(
                    rs.getString("labels_json"), new TypeReference<Map<String, Object>>() {}
            );
            } catch (JsonProcessingException exception) {
            throw new SQLException("Stored metric labels are invalid JSON", exception);
            }
            return new MetricSample(
                rs.getString("metric_type"),
                rs.getString("metric_name"),
                rs.getBigDecimal("value"),
                rs.getString("unit"),
                labels,
                rs.getTimestamp("recorded_at").toInstant()
            );
        }, serverId, limit);
    }
}
