package com.nexusops.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusops.shared.web.ApiResponse;
import com.nexusops.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RequestIdFilter requestIdFilter,
            AuthenticationEntryPoint unauthorizedEntryPoint
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorizedEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/api/v1/agent/**", "/api/v1/system/status", "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(requestIdFilter, SecurityContextHolderFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthenticationEntryPoint unauthorizedEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ApiResponse<>(
                    null,
                    new ApiResponse.Meta((String) request.getAttribute(RequestIdFilter.HEADER)),
                    new ApiResponse.ApiError(
                            HttpStatus.UNAUTHORIZED.toString().replace(" ", "_"),
                            "Authentication required"
                    )
            ));
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
