CREATE TABLE alert_rules (
    id UUID PRIMARY KEY,
    server_id UUID NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    metric_name VARCHAR(120) NOT NULL,
    comparison_operator VARCHAR(20) NOT NULL,
    threshold NUMERIC(20, 6) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT alert_rules_operator_check CHECK (comparison_operator IN ('GREATER_THAN', 'LESS_THAN')),
    CONSTRAINT alert_rules_severity_check CHECK (severity IN ('WARNING', 'CRITICAL')),
    CONSTRAINT alert_rules_server_name_unique UNIQUE (server_id, name)
);

CREATE INDEX alert_rules_server_metric_enabled_idx
    ON alert_rules(server_id, metric_name) WHERE enabled = TRUE;

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    server_id UUID NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
    rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    rule_name VARCHAR(120) NOT NULL,
    metric_name VARCHAR(120) NOT NULL,
    threshold NUMERIC(20, 6) NOT NULL,
    observed_value NUMERIC(20, 6) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT alerts_severity_check CHECK (severity IN ('WARNING', 'CRITICAL')),
    CONSTRAINT alerts_status_check CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE UNIQUE INDEX alerts_one_open_per_rule_idx
    ON alerts(rule_id) WHERE status = 'OPEN';
CREATE INDEX alerts_server_triggered_idx
    ON alerts(server_id, triggered_at DESC);
