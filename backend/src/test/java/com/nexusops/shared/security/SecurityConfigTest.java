package com.nexusops.shared.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusops.shared.web.RequestIdFilter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityConfigTest {
    @Test
    void unauthorizedResponseUsesTheApiErrorEnvelope() throws IOException, ServletException {
        var request = new MockHttpServletRequest();
        request.setAttribute(RequestIdFilter.HEADER, "request-1");
        var response = new MockHttpServletResponse();

        new SecurityConfig().unauthorizedEntryPoint(new ObjectMapper())
                .commence(request, response, new InsufficientAuthenticationException("unauthorized"));

        JsonNode body = new ObjectMapper().readTree(response.getContentAsByteArray());
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("request-1", body.path("meta").path("requestId").asText());
        assertEquals("401_UNAUTHORIZED", body.path("error").path("code").asText());
        assertEquals("Authentication required", body.path("error").path("message").asText());
    }
}
