package com.nexusops.alerts;

import com.nexusops.metrics.MetricSample;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AlertEvaluationService {
    private final AlertRuleRepository ruleRepository;
    private final AlertRepository alertRepository;

    public AlertEvaluationService(AlertRuleRepository ruleRepository, AlertRepository alertRepository) {
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
    }

    public void evaluate(UUID serverId, List<MetricSample> samples) {
        Map<String, List<AlertRule>> rulesByMetric = ruleRepository.findEnabledByServerId(serverId).stream()
                .collect(Collectors.groupingBy(AlertRule::metricName));
        for (MetricSample sample : samples) {
            for (AlertRule rule : rulesByMetric.getOrDefault(sample.metricName(), List.of())) {
                boolean breached = rule.comparison() == AlertRule.Comparison.GREATER_THAN
                        ? sample.value().compareTo(rule.threshold()) > 0
                        : sample.value().compareTo(rule.threshold()) < 0;
                if (breached) {
                    alertRepository.openOrUpdate(serverId, rule, sample);
                } else {
                    alertRepository.resolveOpen(rule.id(), sample.recordedAt());
                }
            }
        }
    }
}
