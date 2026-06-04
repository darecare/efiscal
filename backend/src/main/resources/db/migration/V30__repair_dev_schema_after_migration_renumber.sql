-- Repair dev databases where Flyway recorded old branch-local migrations at V27–V29
-- (preferred language / seed / ensure columns) instead of main's V27 searchshop,
-- V28 email settings, and V29 preferred language. Idempotent on clean databases.
ALTER TABLE org ADD COLUMN IF NOT EXISTS is_searchshopproducts BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE org ADD COLUMN IF NOT EXISTS smtp_server VARCHAR(255);
ALTER TABLE org ADD COLUMN IF NOT EXISTS smtp_port INTEGER;
ALTER TABLE org ADD COLUMN IF NOT EXISTS email_from VARCHAR(255);
ALTER TABLE org ADD COLUMN IF NOT EXISTS smtp_username VARCHAR(255);
ALTER TABLE org ADD COLUMN IF NOT EXISTS smtp_password VARCHAR(255);
ALTER TABLE org ADD COLUMN IF NOT EXISTS smtp_connection_security VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10) DEFAULT NULL;
ALTER TABLE client ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10) DEFAULT NULL;
