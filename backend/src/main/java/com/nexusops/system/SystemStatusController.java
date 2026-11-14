package com.nexusops.system;

import com.nexusops.shared.web.ApiResponse;
import com.nexusops.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {
    private final JdbcTemplate jdbcTemplate;
    private final String version;

    public SystemStatusController(
            JdbcTemplate jdbcTemplate,
            @Value("${nexusops.version:unknown}") String version
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.version = version;
    }

    @GetMapping("/status")
    public ApiResponse<SystemStatus> status(HttpServletRequest request) {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        var status = new SystemStatus("nexusops-backend", "UP", "UP", version);
        return ApiResponse.success(status, requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.HEADER);
    }
}

