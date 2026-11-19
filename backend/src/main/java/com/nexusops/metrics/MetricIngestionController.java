package com.nexusops.metrics;

import com.nexusops.shared.web.ApiResponse;
import com.nexusops.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
public class MetricIngestionController {
    public static final String AGENT_ID_HEADER = "X-Agent-Id";
    public static final String AGENT_CREDENTIAL_HEADER = "X-Agent-Credential";

    private final MetricIngestionService ingestionService;

    public MetricIngestionController(MetricIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(
            @RequestHeader(AGENT_ID_HEADER) String agentId,
            @RequestHeader(AGENT_CREDENTIAL_HEADER) String credential,
            HttpServletRequest request
    ) {
        ingestionService.heartbeat(agentId, credential);
        return success(null, request);
    }

    @GetMapping("/config")
    public ApiResponse<AgentConfig> config(
            @RequestHeader(AGENT_ID_HEADER) String agentId,
            @RequestHeader(AGENT_CREDENTIAL_HEADER) String credential,
            HttpServletRequest request
    ) {
        ingestionService.authenticate(agentId, credential);
        return success(new AgentConfig(30), request);
    }

    @PostMapping("/metrics")
    public ApiResponse<Void> metrics(
            @RequestHeader(AGENT_ID_HEADER) String agentId,
            @RequestHeader(AGENT_CREDENTIAL_HEADER) String credential,
            @Valid @RequestBody MetricBatch batch,
            HttpServletRequest request
    ) {
        ingestionService.ingest(agentId, credential, batch.samples());
        return success(null, request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.success(data, (String) request.getAttribute(RequestIdFilter.HEADER));
    }

    public record MetricBatch(@NotEmpty List<@Valid MetricSample> samples) {}

    public record AgentConfig(int collectionIntervalSeconds) {}
}
