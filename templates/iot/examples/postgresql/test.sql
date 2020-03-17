
-- create index for credentials lookup

CREATE INDEX idx_1 ON devices USING gin (credentials jsonb_path_ops);
CREATE INDEX idx_2 ON devices USING gin (tenant_id, credentials jsonb_path_ops);
