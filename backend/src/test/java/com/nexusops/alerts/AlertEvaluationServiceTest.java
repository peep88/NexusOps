package com.nexusops.alerts;

import com.nexusops.metrics.MetricSample;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertEvaluationServiceTest {
    private final AlertRuleRepository ruleRepository = mock(AlertRuleRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final AlertEvaluationService service =
            new AlertEvaluationService(ruleRepository, alertRepository);

    @Test
    void opensAlertWhenSampleExceedsThreshold() {
        UUID serverId = UUID.randomUUID();
        AlertRule rule = rule(serverId, AlertRule.Comparison.GREATER_THAN);
        MetricSample sample = sample("cpu.usage_percent", "81");
        when(ruleRepository.findEnabledByServerId(serverId)).thenReturn(List.of(rule));

        service.evaluate(serverId, List.of(sample));

        verify(alertRepository).openOrUpdate(serverId, rule, sample);
        verify(alertRepository, never()).resolveOpen(rule.id(), sample.recordedAt());
    }

    @Test
    void resolvesOpenAlertWhenSampleReturnsToThreshold() {
        UUID serverId = UUID.randomUUID();
        AlertRule rule = rule(serverId, AlertRule.Comparison.GREATER_THAN);
        MetricSample sample = sample("cpu.usage_percent", "80");
        when(ruleRepository.findEnabledByServerId(serverId)).thenReturn(List.of(rule));

        service.evaluate(serverId, List.of(sample));

        verify(alertRepository).resolveOpen(rule.id(), sample.recordedAt());
        verify(alertRepository, never()).openOrUpdate(serverId, rule, sample);
    }

    @Test
    void ignoresSamplesForMetricsWithoutRules() {
        UUID serverId = UUID.randomUUID();
        when(ruleRepository.findEnabledByServerId(serverId)).thenReturn(List.of());

        service.evaluate(serverId, List.of(sample("memory.used_bytes", "1024")));

        verify(alertRepository, never()).openOrUpdate(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(alertRepository, never()).resolveOpen(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
    }

    private AlertRule rule(UUID serverId, AlertRule.Comparison comparison) {
        return new AlertRule(
                UUID.randomUUID(), serverId, "High CPU", "cpu.usage_percent",
                comparison, new BigDecimal("80"), AlertRule.Severity.WARNING, true, Instant.now()
        );
    }

    private MetricSample sample(String metricName, String value) {
        return new MetricSample(
                "cpu", metricName, new BigDecimal(value), "percent", Map.of(),
                Instant.parse("2026-11-28T11:00:00Z")
        );
    }
}
