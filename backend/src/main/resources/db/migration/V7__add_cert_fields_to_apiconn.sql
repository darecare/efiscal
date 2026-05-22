-- Add mTLS certificate fields and PAC to apiconn table
ALTER TABLE apiconn ADD COLUMN IF NOT EXISTS cert_data     BYTEA;
ALTER TABLE apiconn ADD COLUMN IF NOT EXISTS cert_password VARCHAR(255);
ALTER TABLE apiconn ADD COLUMN IF NOT EXISTS pac           VARCHAR(10);

