package com.nexusops.metrics;

import com.nexusops.server.AgentCredentialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class MetricIngestionService {
    private final AgentCredentialRepository credentialRepository;
    private final MetricRepository metricRepository;

    public MetricIngestionService(
            AgentCredentialRepository credentialRepository,
            MetricRepository metricRepository
    ) {
        this.credentialRepository = credentialRepository;
        this.metricRepository = metricRepository;
    }

    public UUID authenticate(String agentId, String credential) {
        if (agentId == null || agentId.isBlank() || credential == null || credential.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agent authentication required");
        }
        return credentialRepository.findActiveServerId(agentId, hash(credential))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid agent credential"));
    }

    public void heartbeat(String agentId, String credential) {
        UUID serverId = authenticate(agentId, credential);
        credentialRepository.markServerSeen(serverId);
    }

    public void ingest(String agentId, String credential, List<MetricSample> samples) {
        UUID serverId = authenticate(agentId, credential);
        if (samples == null || samples.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one metric is required");
        }
        if (samples.size() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metric batch exceeds 1000 samples");
        }
        metricRepository.insertBatch(serverId, samples);
        credentialRepository.markServerSeen(serverId);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
