package com.nexusops.alerts;

import com.nexusops.server.ServerService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlertServiceTest {
    private final ServerService serverService = mock(ServerService.class);
    private final AlertRuleRepository ruleRepository = mock(AlertRuleRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final AlertService service =
            new AlertService(serverService, ruleRepository, alertRepository);

    @Test
    void returnsConflictForDuplicateRuleName() {
        UUID serverId = UUID.randomUUID();
        doThrow(new DuplicateKeyException("duplicate")).when(ruleRepository).insert(any(AlertRule.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createRule(
                        serverId, "High CPU", "cpu.usage_percent",
                        AlertRule.Comparison.GREATER_THAN, new BigDecimal("80"),
                        AlertRule.Severity.WARNING
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void ruleUpdatesResolveAnyCurrentlyOpenAlert() {
        UUID serverId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        AlertRule current = new AlertRule(
                ruleId, serverId, "High CPU", "cpu.usage_percent",
                AlertRule.Comparison.GREATER_THAN, new BigDecimal("80"),
                AlertRule.Severity.WARNING, true, Instant.parse("2026-11-28T10:00:00Z")
        );
        org.mockito.Mockito.when(ruleRepository.findByServerAndId(serverId, ruleId))
                .thenReturn(java.util.Optional.of(current));

        service.updateRule(
                serverId, ruleId, "High CPU", "cpu.usage_percent",
                AlertRule.Comparison.GREATER_THAN, new BigDecimal("80"),
                AlertRule.Severity.WARNING, false
        );

        verify(alertRepository).resolveOpen(org.mockito.ArgumentMatchers.eq(ruleId), any(Instant.class));
        verify(ruleRepository).update(any(AlertRule.class));
    }
}
