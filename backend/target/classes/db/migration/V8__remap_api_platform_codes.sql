-- Remap api_platform values to 2-letter codes and shrink column to VARCHAR(10)

-- 1. Update existing full-name values to their 2-letter codes
UPDATE apiconn SET api_platform = 'MP' WHERE api_platform = 'MERCHANTPRO';
UPDATE apiconn SET api_platform = 'WO' WHERE api_platform = 'WOOCOMMERCE';
UPDATE apiconn SET api_platform = 'SH' WHERE api_platform = 'SHOPIFY';
UPDATE apiconn SET api_platform = 'FS' WHERE api_platform IN ('OTHER', 'EFISCAL', 'FISCAL_SYSTEM');

-- 2. Shrink column type to VARCHAR(10) to enforce the constraint going forward
ALTER TABLE apiconn ALTER COLUMN api_platform TYPE VARCHAR(10);
