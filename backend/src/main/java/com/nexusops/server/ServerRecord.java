package com.nexusops.server;

import java.time.Instant;
import java.util.UUID;

public record ServerRecord(
        UUID id,
        String name,
        String hostname,
        String environment,
        String description,
        String status,
        String agentId,
        Instant lastSeenAt
) {}
