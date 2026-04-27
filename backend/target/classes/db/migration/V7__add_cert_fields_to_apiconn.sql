-- Add mTLS certificate fields and PAC to apiconn table
ALTER TABLE apiconn
    ADD COLUMN IF NOT EXISTS cert_data     BYTEA,
    ADD COLUMN IF NOT EXISTS cert_password VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pac           VARCHAR(10);
