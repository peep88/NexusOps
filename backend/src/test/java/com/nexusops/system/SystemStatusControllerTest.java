package com.nexusops.system;

import org.junit.jupiter.api.Test;
import com.nexusops.shared.web.RequestIdFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemStatusControllerTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final MockMvc mockMvc = standaloneSetup(new SystemStatusController(jdbcTemplate, "0.1.0"))
            .addFilters(new RequestIdFilter())
            .build();

    @Test
    void returnsStatusAndPreservesRequestId() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mockMvc.perform(get("/api/v1/system/status").header("X-Request-Id", "test-request"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request"))
                .andExpect(jsonPath("$.data.service").value("nexusops-backend"))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.meta.requestId").value("test-request"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(jdbcTemplate).queryForObject(eq("SELECT 1"), eq(Integer.class));
    }
}

