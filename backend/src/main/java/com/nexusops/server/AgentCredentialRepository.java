package com.nexusops.server;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AgentCredentialRepository {
    private final JdbcTemplate jdbcTemplate;

    public AgentCredentialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(UUID id, UUID serverId, String credentialHash) {
        jdbcTemplate.update("""
                INSERT INTO agent_credentials (id, server_id, credential_hash)
                VALUES (?, ?, ?)
                """, id, serverId, credentialHash);
    }

    public void revokeActive(UUID serverId) {
        jdbcTemplate.update("""
                UPDATE agent_credentials
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE server_id = ? AND revoked_at IS NULL
                """, serverId);
    }
}
