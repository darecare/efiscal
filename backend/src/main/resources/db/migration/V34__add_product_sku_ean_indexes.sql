CREATE INDEX idx_product_org_sku
    ON product(org_id, lower(sku))
    WHERE deleted_at IS NULL AND sku IS NOT NULL;

CREATE INDEX idx_product_org_ean
    ON product(org_id, ean)
    WHERE deleted_at IS NULL AND ean IS NOT NULL;
