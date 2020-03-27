-- execute the file "drop.sql" before this one

DROP INDEX idx_credentials_psk;
DROP INDEX idx_credentials_hashed_password;
DROP INDEX idx_credentials_x509 ON devices;
