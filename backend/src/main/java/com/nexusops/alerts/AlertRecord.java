package com.nexusops.alerts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertRecord(
        UUID id,
        UUID serverId,
        UUID ruleId,
        String ruleName,
        String metricName,
        BigDecimal threshold,
        BigDecimal observedValue,
        AlertRule.Severity severity,
        Status status,
        Instant triggeredAt,
        Instant resolvedAt
) {
    public enum Status {
        OPEN,
        RESOLVED
    }
}
