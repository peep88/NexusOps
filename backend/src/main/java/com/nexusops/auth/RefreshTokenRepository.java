package com.nexusops.auth;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public class RefreshTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(UUID id, UUID userId, String tokenHash, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """, id, userId, tokenHash, OffsetDateTime.ofInstant(expiresAt, java.time.ZoneOffset.UTC));
    }

    public UUID findActiveUserId(String tokenHash) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT user_id FROM refresh_tokens
                    WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP
                    """, (rs, rowNum) -> rs.getObject("user_id", UUID.class), tokenHash);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    public void revoke(String tokenHash) {
        jdbcTemplate.update("""
                UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP
                WHERE token_hash = ? AND revoked_at IS NULL
                """, tokenHash);
    }
}
