CREATE TABLE schema_metadata (
    key VARCHAR(100) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_metadata (key, value)
VALUES ('foundation_version', '1');

