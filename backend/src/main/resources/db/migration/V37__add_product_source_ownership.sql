ALTER TABLE product ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE product ADD COLUMN sync_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE product ADD COLUMN hidden_at TIMESTAMPTZ NULL;

UPDATE product SET source_type = 'MERCHANTPRO' WHERE mp_product_id IS NOT NULL;

UPDATE product
SET hidden_at = deleted_at, deleted_at = NULL
WHERE mp_product_id IS NOT NULL AND deleted_at IS NOT NULL;

DROP INDEX IF EXISTS idx_product_org;
CREATE INDEX idx_product_org ON product(org_id) WHERE deleted_at IS NULL AND hidden_at IS NULL;

DROP INDEX IF EXISTS idx_product_org_sku;
CREATE INDEX idx_product_org_sku
    ON product(org_id, lower(sku))
    WHERE deleted_at IS NULL AND hidden_at IS NULL AND sku IS NOT NULL;

DROP INDEX IF EXISTS idx_product_org_ean;
CREATE INDEX idx_product_org_ean
    ON product(org_id, ean)
    WHERE deleted_at IS NULL AND hidden_at IS NULL AND ean IS NOT NULL;
