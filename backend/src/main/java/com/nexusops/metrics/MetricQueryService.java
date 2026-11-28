package com.nexusops.metrics;

import com.nexusops.server.ServerService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class MetricQueryService {
    private static final int MAX_LIMIT = 1000;

    private final ServerService serverService;
    private final MetricRepository metricRepository;

    public MetricQueryService(ServerService serverService, MetricRepository metricRepository) {
        this.serverService = serverService;
        this.metricRepository = metricRepository;
    }

    public List<MetricSample> findLatest(UUID serverId, int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Metric limit must be between 1 and " + MAX_LIMIT
            );
        }
        serverService.get(serverId);
        return metricRepository.findLatestForServer(serverId, limit);
    }
}
