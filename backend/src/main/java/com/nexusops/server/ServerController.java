package com.nexusops.server;

import com.nexusops.shared.web.ApiResponse;
import com.nexusops.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/servers")
public class ServerController {
    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<List<ServerRecord>> list(HttpServletRequest request) {
        return success(serverService.list(), request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<ServerRecord> get(@PathVariable UUID id, HttpServletRequest request) {
        return success(serverService.get(id), request);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<ServerService.RegisteredServer> register(
            @Valid @RequestBody RegisterServerRequest serverRequest,
            HttpServletRequest request
    ) {
        return success(serverService.register(
                serverRequest.name(), serverRequest.hostname(),
                serverRequest.environment(), serverRequest.description()
        ), request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<ServerRecord> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServerRequest serverRequest,
            HttpServletRequest request
    ) {
        return success(serverService.update(
                id, serverRequest.name(), serverRequest.hostname(),
                serverRequest.environment(), serverRequest.description()
        ), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        serverService.delete(id);
        return success(null, request);
    }

    @PostMapping("/{id}/credentials/rotate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ServerService.CredentialRotation> rotateCredential(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return success(serverService.rotateCredential(id), request);
    }

    @PostMapping("/{id}/credentials/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> revokeCredential(@PathVariable UUID id, HttpServletRequest request) {
        serverService.revokeCredential(id);
        return success(null, request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.success(data, (String) request.getAttribute(RequestIdFilter.HEADER));
    }

    public record RegisterServerRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 255) String hostname,
            @NotBlank @Size(max = 80) String environment,
            @Size(max = 500) String description
    ) {}

    public record UpdateServerRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 255) String hostname,
            @NotBlank @Size(max = 80) String environment,
            @Size(max = 500) String description
    ) {}
}
