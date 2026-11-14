package com.nexusops.auth;

import com.nexusops.shared.security.Role;

import java.time.Instant;
import java.util.UUID;

public record UserRecord(
        UUID id,
        String email,
        String passwordHash,
        String displayName,
        Role role,
        Instant disabledAt
) {}
