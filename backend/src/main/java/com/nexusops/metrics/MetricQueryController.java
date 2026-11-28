package com.nexusops.metrics;

import com.nexusops.shared.web.ApiResponse;
import com.nexusops.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/metrics")
public class MetricQueryController {
    private final MetricQueryService metricQueryService;

    public MetricQueryController(MetricQueryService metricQueryService) {
        this.metricQueryService = metricQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<List<MetricSample>> latest(
            @PathVariable UUID serverId,
            @RequestParam(defaultValue = "300") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                metricQueryService.findLatest(serverId, limit),
                (String) request.getAttribute(RequestIdFilter.HEADER)
        );
    }
}
