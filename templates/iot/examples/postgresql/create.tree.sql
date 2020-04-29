
-- execute the file "create.sql" before this one

-- create an index for each credentials type
CREATE INDEX idx_credentials_psk ON devices using gin((credentials->'psk') jsonb_ops);
CREATE INDEX idx_credentials_hashed_password ON devices using gin ((credentials->'hashed-password') jsonb_ops);
CREATE INDEX idx_credentials_x509 ON devices using gin ((credentials->'x509-cert') jsonb_ops);
