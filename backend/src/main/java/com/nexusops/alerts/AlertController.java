package com.nexusops.alerts;

import com.nexusops.shared.web.ApiResponse;
import com.nexusops.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/servers/{serverId}")
public class AlertController {
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alert-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<List<AlertRule>> listRules(
            @PathVariable UUID serverId,
            HttpServletRequest request
    ) {
        return success(alertService.listRules(serverId), request);
    }

    @PostMapping("/alert-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<AlertRule> createRule(
            @PathVariable UUID serverId,
            @Valid @RequestBody CreateAlertRuleRequest rule,
            HttpServletRequest request
    ) {
        return success(alertService.createRule(
                serverId, rule.name(), rule.metricName(), rule.comparison(),
                rule.threshold(), rule.severity()
        ), request);
    }

    @PutMapping("/alert-rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<AlertRule> updateRule(
            @PathVariable UUID serverId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateAlertRuleRequest rule,
            HttpServletRequest request
    ) {
        return success(alertService.updateRule(
                serverId, ruleId, rule.name(), rule.metricName(), rule.comparison(),
                rule.threshold(), rule.severity(), rule.enabled()
        ), request);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<List<AlertRecord>> listAlerts(
            @PathVariable UUID serverId,
            HttpServletRequest request
    ) {
        return success(alertService.listAlerts(serverId), request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.success(data, (String) request.getAttribute(RequestIdFilter.HEADER));
    }

    public record CreateAlertRuleRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String metricName,
            @NotNull AlertRule.Comparison comparison,
            @NotNull @Digits(integer = 14, fraction = 6) BigDecimal threshold,
            @NotNull AlertRule.Severity severity
    ) {}

    public record UpdateAlertRuleRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String metricName,
            @NotNull AlertRule.Comparison comparison,
            @NotNull @Digits(integer = 14, fraction = 6) BigDecimal threshold,
            @NotNull AlertRule.Severity severity,
            @NotNull Boolean enabled
    ) {}
}
