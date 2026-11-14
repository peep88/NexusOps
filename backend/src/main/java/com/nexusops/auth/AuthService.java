package com.nexusops.auth;

import com.nexusops.shared.security.JwtService;
import com.nexusops.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTokenLifetime;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${nexusops.security.refresh-token-days:30}") long refreshTokenDays
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenLifetime = Duration.ofDays(refreshTokenDays);
    }

    public AuthTokens register(String email, String password, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        UserRecord user = new UserRecord(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(password),
                displayName.trim(),
                userRepository.isEmpty() ? Role.ADMIN : Role.VIEWER,
                null
        );
        try {
            userRepository.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        return issueTokens(user);
    }

    public AuthTokens login(String email, String password) {
        UserRecord user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (user.disabledAt() != null || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueTokens(user);
    }

    public AuthTokens refresh(String refreshToken) {
        UUID userId = refreshTokenRepository.findActiveUserId(hash(refreshToken));
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        UserRecord user = userRepository.findById(userId)
                .filter(candidate -> candidate.disabledAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        refreshTokenRepository.revoke(hash(refreshToken));
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.revoke(hash(refreshToken));
    }

    public UserProfile currentUser(UUID userId) {
        UserRecord user = userRepository.findById(userId)
                .filter(candidate -> candidate.disabledAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new UserProfile(user.id(), user.email(), user.displayName(), user.role());
    }

    private AuthTokens issueTokens(UserRecord user) {
        String refreshToken = randomToken();
        refreshTokenRepository.insert(
                UUID.randomUUID(),
                user.id(),
                hash(refreshToken),
                Instant.now().plus(refreshTokenLifetime)
        );
        return new AuthTokens(
                jwtService.createAccessToken(user.id(), user.email(), user.role()),
                refreshToken,
                user.id(),
                user.email(),
                user.displayName(),
                user.role()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record AuthTokens(
            String accessToken,
            String refreshToken,
            UUID userId,
            String email,
            String displayName,
            Role role
    ) {}

    public record UserProfile(UUID id, String email, String displayName, Role role) {}
}
