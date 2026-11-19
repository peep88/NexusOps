package com.nexusops.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
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
}
