package com.nexusops.server;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ServerService {
    private final ServerRepository serverRepository;
    private final AgentCredentialRepository credentialRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ServerService(ServerRepository serverRepository, AgentCredentialRepository credentialRepository) {
        this.serverRepository = serverRepository;
        this.credentialRepository = credentialRepository;
    }

    public List<ServerRecord> list() {
        return serverRepository.findAll();
    }

    public ServerRecord get(UUID id) {
        try {
            return serverRepository.findById(id);
        } catch (DataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Server not found");
        }
    }

    public RegisteredServer register(String name, String hostname, String environment, String description) {
        UUID serverId = UUID.randomUUID();
        String agentId = "agent-" + UUID.randomUUID();
        String credential = randomCredential();
        ServerRecord server = new ServerRecord(
                serverId, name.trim(), hostname.trim(), environment.trim(),
                description == null ? null : description.trim(), "OFFLINE", agentId, null
        );
        try {
            serverRepository.insert(server);
            credentialRepository.insert(UUID.randomUUID(), serverId, hash(credential));
        } catch (DataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A server with these details already exists");
        }
        return new RegisteredServer(server, agentId, credential);
    }

    public ServerRecord update(UUID id, String name, String hostname, String environment, String description) {
        get(id);
        serverRepository.update(id, name.trim(), hostname.trim(), environment.trim(),
                description == null ? null : description.trim());
        return get(id);
    }

    public void delete(UUID id) {
        get(id);
        serverRepository.delete(id);
    }

    public CredentialRotation rotateCredential(UUID serverId) {
        ServerRecord server = get(serverId);
        credentialRepository.revokeActive(serverId);
        String credential = randomCredential();
        credentialRepository.insert(UUID.randomUUID(), server.id(), hash(credential));
        return new CredentialRotation(server.id(), server.agentId(), credential);
    }

    public void revokeCredential(UUID serverId) {
        get(serverId);
        credentialRepository.revokeActive(serverId);
    }

    private String randomCredential() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "nxs_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record RegisteredServer(ServerRecord server, String agentId, String agentCredential) {}

    public record CredentialRotation(UUID serverId, String agentId, String agentCredential) {}
}
