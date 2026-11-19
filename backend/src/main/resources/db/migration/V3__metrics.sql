CREATE TABLE metric_samples (
    id BIGSERIAL PRIMARY KEY,
    server_id UUID NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
    metric_type VARCHAR(40) NOT NULL,
    metric_name VARCHAR(120) NOT NULL,
    value NUMERIC(20, 6) NOT NULL,
    unit VARCHAR(30),
    labels JSONB NOT NULL DEFAULT '{}'::jsonb,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX metric_samples_server_time_idx
    ON metric_samples(server_id, recorded_at DESC);
CREATE INDEX metric_samples_name_time_idx
    ON metric_samples(metric_name, recorded_at DESC);
