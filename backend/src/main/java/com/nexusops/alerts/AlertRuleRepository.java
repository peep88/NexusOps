package com.nexusops.alerts;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AlertRuleRepository {
    private final JdbcTemplate jdbcTemplate;

    public AlertRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(AlertRule rule) {
        jdbcTemplate.update("""
                INSERT INTO alert_rules
                    (id, server_id, name, metric_name, comparison_operator, threshold, severity, enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, rule.id(), rule.serverId(), rule.name(), rule.metricName(),
                rule.comparison().name(), rule.threshold(), rule.severity().name(), rule.enabled());
    }

    public List<AlertRule> findByServerId(UUID serverId) {
        return jdbcTemplate.query("""
                SELECT id, server_id, name, metric_name, comparison_operator, threshold, severity, enabled, created_at
                FROM alert_rules
                WHERE server_id = ?
                ORDER BY created_at DESC, name
                """, this::map, serverId);
    }

    public List<AlertRule> findEnabledByServerId(UUID serverId) {
        return jdbcTemplate.query("""
                SELECT id, server_id, name, metric_name, comparison_operator, threshold, severity, enabled, created_at
                FROM alert_rules
                WHERE server_id = ? AND enabled = TRUE
                """, this::map, serverId);
    }

    public Optional<AlertRule> findByServerAndId(UUID serverId, UUID ruleId) {
        List<AlertRule> rules = jdbcTemplate.query("""
                SELECT id, server_id, name, metric_name, comparison_operator, threshold, severity, enabled, created_at
                FROM alert_rules
                WHERE server_id = ? AND id = ?
                """, this::map, serverId, ruleId);
        return rules.stream().findFirst();
    }

    public void update(AlertRule rule) {
        jdbcTemplate.update("""
                UPDATE alert_rules
                SET name = ?, metric_name = ?, comparison_operator = ?, threshold = ?,
                    severity = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP
                WHERE server_id = ? AND id = ?
                """, rule.name(), rule.metricName(), rule.comparison().name(), rule.threshold(),
                rule.severity().name(), rule.enabled(), rule.serverId(), rule.id());
    }

    private AlertRule map(ResultSet rs, int rowNum) throws SQLException {
        return new AlertRule(
                rs.getObject("id", UUID.class),
                rs.getObject("server_id", UUID.class),
                rs.getString("name"),
                rs.getString("metric_name"),
                AlertRule.Comparison.valueOf(rs.getString("comparison_operator")),
                rs.getBigDecimal("threshold"),
                AlertRule.Severity.valueOf(rs.getString("severity")),
                rs.getBoolean("enabled"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
