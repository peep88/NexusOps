package com.nexusops.auth;

import com.nexusops.shared.security.Role;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(UserRecord user) {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, display_name, role)
                VALUES (?, ?, ?, ?, ?)
                """, user.id(), user.email(), user.passwordHash(), user.displayName(), user.role().name());
    }

    public boolean isEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        return count != null && count == 0;
    }

    public Optional<UserRecord> findByEmail(String email) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, email, password_hash, display_name, role, disabled_at
                    FROM users WHERE email = ?
                    """, (rs, rowNum) -> new UserRecord(
                    rs.getObject("id", UUID.class),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    rs.getString("display_name"),
                    Role.valueOf(rs.getString("role")),
                    rs.getObject("disabled_at", java.time.OffsetDateTime.class) == null
                            ? null : rs.getObject("disabled_at", java.time.OffsetDateTime.class).toInstant()
            ), email));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<UserRecord> findById(UUID id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, email, password_hash, display_name, role, disabled_at
                    FROM users WHERE id = ?
                    """, (rs, rowNum) -> new UserRecord(
                    rs.getObject("id", UUID.class),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    rs.getString("display_name"),
                    Role.valueOf(rs.getString("role")),
                    rs.getObject("disabled_at", java.time.OffsetDateTime.class) == null
                            ? null : rs.getObject("disabled_at", java.time.OffsetDateTime.class).toInstant()
            ), id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }
}
