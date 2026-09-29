-- Org toggle: add order shipping_amount as a fiscal bill line item (from-order flow).
ALTER TABLE org
    ADD COLUMN IF NOT EXISTS include_shipment BOOLEAN NOT NULL DEFAULT TRUE;

-- Product flag: the product used as the shipping line item.
ALTER TABLE product
    ADD COLUMN IF NOT EXISTS is_shipment BOOLEAN NOT NULL DEFAULT FALSE;

-- At most one non-deleted shipping product per organization.
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_org_shipment
    ON product (org_id)
    WHERE is_shipment = TRUE AND deleted_at IS NULL;
