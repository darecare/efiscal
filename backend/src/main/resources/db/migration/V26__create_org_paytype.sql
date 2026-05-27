-- Organization Payment Type mapping table
-- Defines which payment types are allowed for each organization
CREATE TABLE org_paytype (
    org_id        BIGINT NOT NULL,
    payment_type  INTEGER NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (org_id, payment_type),
    CONSTRAINT fk_org_paytype_org FOREIGN KEY (org_id) REFERENCES org (org_id) ON DELETE CASCADE
);

CREATE INDEX idx_org_paytype_org ON org_paytype (org_id);
