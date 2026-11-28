package com.nexusops.server;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
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

    public Optional<UUID> findActiveServerId(String agentId, String credentialHash) {
        UUID serverId = jdbcTemplate.query("""
                SELECT s.id
                FROM agent_credentials ac
                JOIN servers s ON s.id = ac.server_id
                WHERE s.agent_id = ? AND ac.credential_hash = ?
                  AND ac.revoked_at IS NULL
                """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null,
                agentId, credentialHash);
        if (serverId == null) {
            return Optional.empty();
        }
        jdbcTemplate.update("""
                UPDATE agent_credentials
                SET last_used_at = CURRENT_TIMESTAMP
                WHERE server_id = ? AND credential_hash = ? AND revoked_at IS NULL
                """, serverId, credentialHash);
        return Optional.of(serverId);
    }

    public void markServerSeen(UUID serverId) {
        jdbcTemplate.update("""
                UPDATE servers
                SET status = 'ONLINE', last_seen_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, serverId);
    }
}
