package com.nexusops.server;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ServerRepository {
    private final JdbcTemplate jdbcTemplate;

    public ServerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ServerRecord insert(ServerRecord server) {
        jdbcTemplate.update("""
                INSERT INTO servers (id, name, hostname, environment, description, agent_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, server.id(), server.name(), server.hostname(), server.environment(),
                server.description(), server.agentId());
        return server;
    }

    public List<ServerRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT id, name, hostname, environment, description, status, agent_id, last_seen_at
                FROM servers ORDER BY name
                """, this::map);
    }

    public ServerRecord findById(UUID id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, name, hostname, environment, description, status, agent_id, last_seen_at
                FROM servers WHERE id = ?
                """, this::map, id);
    }

    public void update(UUID id, String name, String hostname, String environment, String description) {
        jdbcTemplate.update("""
                UPDATE servers
                SET name = ?, hostname = ?, environment = ?, description = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, name, hostname, environment, description, id);
    }

    public void delete(UUID id) {
        jdbcTemplate.update("DELETE FROM servers WHERE id = ?", id);
    }

    private ServerRecord map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        OffsetDateTime lastSeenAt = rs.getObject("last_seen_at", OffsetDateTime.class);
        return new ServerRecord(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("hostname"),
                rs.getString("environment"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getString("agent_id"),
                lastSeenAt == null ? null : lastSeenAt.toInstant()
        );
    }
}
