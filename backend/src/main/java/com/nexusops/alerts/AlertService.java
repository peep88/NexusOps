package com.nexusops.alerts;

import com.nexusops.server.ServerService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {
    private final ServerService serverService;
    private final AlertRuleRepository ruleRepository;
    private final AlertRepository alertRepository;

    public AlertService(
            ServerService serverService,
            AlertRuleRepository ruleRepository,
            AlertRepository alertRepository
    ) {
        this.serverService = serverService;
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
    }

    public List<AlertRule> listRules(UUID serverId) {
        serverService.get(serverId);
        return ruleRepository.findByServerId(serverId);
    }

    @Transactional
    public AlertRule createRule(
            UUID serverId,
            String name,
            String metricName,
            AlertRule.Comparison comparison,
            BigDecimal threshold,
            AlertRule.Severity severity
    ) {
        serverService.get(serverId);
        AlertRule rule = new AlertRule(
                UUID.randomUUID(), serverId, name.trim(), metricName.trim(),
                comparison, threshold, severity, true, Instant.now()
        );
        try {
            ruleRepository.insert(rule);
        } catch (DuplicateKeyException exception) {
            throw duplicateRuleName();
        }
        return ruleRepository.findByServerAndId(serverId, rule.id()).orElseThrow();
    }

    @Transactional
    public AlertRule updateRule(
            UUID serverId,
            UUID ruleId,
            String name,
            String metricName,
            AlertRule.Comparison comparison,
            BigDecimal threshold,
            AlertRule.Severity severity,
            boolean enabled
    ) {
        serverService.get(serverId);
        AlertRule current = ruleRepository.findByServerAndId(serverId, ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert rule not found"));
        AlertRule updated = new AlertRule(
                current.id(), serverId, name.trim(), metricName.trim(),
                comparison, threshold, severity, enabled, current.createdAt()
        );
        try {
            alertRepository.resolveOpen(ruleId, Instant.now());
            ruleRepository.update(updated);
        } catch (DuplicateKeyException exception) {
            throw duplicateRuleName();
        }
        return ruleRepository.findByServerAndId(serverId, ruleId).orElseThrow();
    }

    public List<AlertRecord> listAlerts(UUID serverId) {
        serverService.get(serverId);
        return alertRepository.findLatestByServerId(serverId);
    }

    private ResponseStatusException duplicateRuleName() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "An alert rule with this name already exists");
    }
}
