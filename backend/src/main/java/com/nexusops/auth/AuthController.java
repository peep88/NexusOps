package com.nexusops.auth;

import com.nexusops.shared.web.ApiResponse;
import com.nexusops.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthService.AuthTokens> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        return success(authService.register(request.email(), request.password(), request.displayName()), servletRequest);
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.AuthTokens> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return success(authService.login(request.email(), request.password()), servletRequest);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthService.AuthTokens> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest servletRequest
    ) {
        return success(authService.refresh(request.refreshToken()), servletRequest);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.logout(request.refreshToken());
        return success(null, servletRequest);
    }

    @GetMapping("/me")
    public ApiResponse<AuthService.UserProfile> me(Authentication authentication, HttpServletRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return success(authService.currentUser(userId), request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.success(data, (String) request.getAttribute(RequestIdFilter.HEADER));
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 12, max = 100) String password,
            @NotBlank @Size(max = 120) String displayName
    ) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

}
