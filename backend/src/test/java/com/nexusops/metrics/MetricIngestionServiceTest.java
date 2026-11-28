package com.nexusops.metrics;

import com.nexusops.alerts.AlertEvaluationService;
import com.nexusops.server.AgentCredentialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricIngestionServiceTest {
    private final AgentCredentialRepository credentialRepository =
            mock(AgentCredentialRepository.class);
    private final MetricRepository metricRepository =
            mock(MetricRepository.class);
    private final AlertEvaluationService alertEvaluationService =
            mock(AlertEvaluationService.class);
    private final MetricIngestionService service =
            new MetricIngestionService(credentialRepository, metricRepository, alertEvaluationService);

    @Test
    void ingestsAuthenticatedMetricBatch() {
        UUID serverId = UUID.randomUUID();
        when(credentialRepository.findActiveServerId(
                org.mockito.ArgumentMatchers.eq("agent-1"),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(java.util.Optional.of(serverId));

        List<MetricSample> samples = List.of(new MetricSample(
                "cpu", "cpu.usage_percent", new BigDecimal("42.50"),
                "percent", Map.of(), Instant.parse("2026-11-19T22:00:00Z")
        ));

        service.ingest("agent-1", "nxs_test", samples);

        verify(metricRepository).insertBatch(serverId, samples);
        verify(alertEvaluationService).evaluate(serverId, samples);
        verify(credentialRepository).markServerSeen(serverId);
    }

    @Test
    void rejectsEmptyBatch() {
        UUID serverId = UUID.randomUUID();
        when(credentialRepository.findActiveServerId(
                org.mockito.ArgumentMatchers.eq("agent-1"),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(java.util.Optional.of(serverId));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.ingest("agent-1", "nxs_test", List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rejectsMissingCredentials() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.authenticate("", "")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
