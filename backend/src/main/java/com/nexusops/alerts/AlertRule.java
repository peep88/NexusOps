package com.nexusops.alerts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertRule(
        UUID id,
        UUID serverId,
        String name,
        String metricName,
        Comparison comparison,
        BigDecimal threshold,
        Severity severity,
        boolean enabled,
        Instant createdAt
) {
    public enum Comparison {
        GREATER_THAN,
        LESS_THAN
    }

    public enum Severity {
        WARNING,
        CRITICAL
    }
}
