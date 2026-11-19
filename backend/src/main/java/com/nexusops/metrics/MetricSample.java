package com.nexusops.metrics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record MetricSample(
        @NotBlank @Size(max = 40) String metricType,
        @NotBlank @Size(max = 120) String metricName,
        @NotNull BigDecimal value,
        @Size(max = 30) String unit,
        @NotNull Map<String, Object> labels,
        @NotNull Instant recordedAt
) {}
