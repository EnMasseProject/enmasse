
-- execute the file "create.sql" before this one

-- create an index for each credentials type
CREATE INDEX idx_credentials_psk ON devices ((credentials->'psk'));
CREATE INDEX idx_credentials_hashed_password ON devices ((credentials->'hashed-password'));
CREATE INDEX idx_credentials_x509 ON devices ((credentials->'x509-cert'));
