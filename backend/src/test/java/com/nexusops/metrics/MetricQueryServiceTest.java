package com.nexusops.metrics;

import com.nexusops.server.ServerService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MetricQueryServiceTest {
    private final ServerService serverService = mock(ServerService.class);
    private final MetricRepository metricRepository = mock(MetricRepository.class);
    private final MetricQueryService service = new MetricQueryService(serverService, metricRepository);

    @Test
    void rejectsLimitsOutsideTheSupportedRange() {
        UUID serverId = UUID.randomUUID();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findLatest(serverId, 1001)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(serverService, metricRepository);
    }

    @Test
    void checksServerExistsBeforeQueryingSamples() {
        UUID serverId = UUID.randomUUID();

        service.findLatest(serverId, 300);

        verify(serverService).get(serverId);
        verify(metricRepository).findLatestForServer(serverId, 300);
    }
}
