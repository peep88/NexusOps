package com.nexusops.alerts;

import com.nexusops.metrics.MetricSample;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AlertRepository {
    private static final int ALERT_HISTORY_LIMIT = 100;

    private final JdbcTemplate jdbcTemplate;

    public AlertRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void openOrUpdate(UUID serverId, AlertRule rule, MetricSample sample) {
        jdbcTemplate.update("""
                INSERT INTO alerts (
                    id, server_id, rule_id, rule_name, metric_name, threshold,
                    observed_value, severity, status, triggered_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?)
                ON CONFLICT (rule_id) WHERE status = 'OPEN'
                DO UPDATE SET observed_value = EXCLUDED.observed_value, updated_at = CURRENT_TIMESTAMP
                """, UUID.randomUUID(), serverId, rule.id(), rule.name(), rule.metricName(),
                rule.threshold(), sample.value(), rule.severity().name(),
                java.sql.Timestamp.from(sample.recordedAt()));
    }

    public void resolveOpen(UUID ruleId, Instant resolvedAt) {
        jdbcTemplate.update("""
                UPDATE alerts
                SET status = 'RESOLVED', resolved_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE rule_id = ? AND status = 'OPEN'
                """, java.sql.Timestamp.from(resolvedAt), ruleId);
    }

    public List<AlertRecord> findLatestByServerId(UUID serverId) {
        return jdbcTemplate.query("""
                SELECT id, server_id, rule_id, rule_name, metric_name, threshold,
                       observed_value, severity, status, triggered_at, resolved_at
                FROM alerts
                WHERE server_id = ?
                ORDER BY triggered_at DESC, id DESC
                LIMIT ?
                """, this::map, serverId, ALERT_HISTORY_LIMIT);
    }

    private AlertRecord map(ResultSet rs, int rowNum) throws SQLException {
        java.sql.Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        return new AlertRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("server_id", UUID.class),
                rs.getObject("rule_id", UUID.class),
                rs.getString("rule_name"),
                rs.getString("metric_name"),
                rs.getBigDecimal("threshold"),
                rs.getBigDecimal("observed_value"),
                AlertRule.Severity.valueOf(rs.getString("severity")),
                AlertRecord.Status.valueOf(rs.getString("status")),
                rs.getTimestamp("triggered_at").toInstant(),
                resolvedAt == null ? null : resolvedAt.toInstant()
        );
    }
}
