-- Convert fiscalbill primary key and related foreign keys from VARCHAR to BIGINT.
-- This migration preserves links by creating a temporary ID map per existing fiscal bill row.

-- 1) Drop existing FK constraints to allow type migration.
ALTER TABLE fiscalbill_idempotency_keys DROP CONSTRAINT IF EXISTS fk_fiscalbill_idempotency_bill;
ALTER TABLE fiscalbill_idempotency_keys DROP CONSTRAINT IF EXISTS fk_fiscal_idempotency_bill;
ALTER TABLE fiscalbillpay DROP CONSTRAINT IF EXISTS fk_fiscalbillpay_bill;
ALTER TABLE fiscalbillline DROP CONSTRAINT IF EXISTS fk_fiscalbillline_bill;
ALTER TABLE fiscalbilltax DROP CONSTRAINT IF EXISTS fk_fiscalbilltax_bill;

-- 2) Add new BIGINT key column on fiscalbill and generate IDs for existing rows.
ALTER TABLE fiscalbill ADD COLUMN IF NOT EXISTS fiscalbill_id_new BIGINT;

CREATE SEQUENCE IF NOT EXISTS fiscalbill_id_seq;
ALTER SEQUENCE fiscalbill_id_seq OWNED BY fiscalbill.fiscalbill_id_new;
ALTER TABLE fiscalbill ALTER COLUMN fiscalbill_id_new SET DEFAULT nextval('fiscalbill_id_seq');

UPDATE fiscalbill
SET fiscalbill_id_new = nextval('fiscalbill_id_seq')
WHERE fiscalbill_id_new IS NULL;

SELECT setval('fiscalbill_id_seq', COALESCE((SELECT MAX(fiscalbill_id_new) FROM fiscalbill), 1), true);

-- 3) Add new BIGINT FK columns and migrate values using old -> new mapping.
ALTER TABLE fiscalbillpay ADD COLUMN IF NOT EXISTS fiscalbill_id_new BIGINT;
ALTER TABLE fiscalbillline ADD COLUMN IF NOT EXISTS fiscalbill_id_new BIGINT;
ALTER TABLE fiscalbilltax ADD COLUMN IF NOT EXISTS fiscalbill_id_new BIGINT;
ALTER TABLE fiscalbill_idempotency_keys ADD COLUMN IF NOT EXISTS fiscalbill_id_new BIGINT;

UPDATE fiscalbillpay p
SET fiscalbill_id_new = f.fiscalbill_id_new
FROM fiscalbill f
WHERE p.fiscalbill_id = f.fiscalbill_id::text;

UPDATE fiscalbillline l
SET fiscalbill_id_new = f.fiscalbill_id_new
FROM fiscalbill f
WHERE l.fiscalbill_id = f.fiscalbill_id::text;

UPDATE fiscalbilltax t
SET fiscalbill_id_new = f.fiscalbill_id_new
FROM fiscalbill f
WHERE t.fiscalbill_id = f.fiscalbill_id::text;

UPDATE fiscalbill_idempotency_keys k
SET fiscalbill_id_new = f.fiscalbill_id_new
FROM fiscalbill f
WHERE k.fiscalbill_id = f.fiscalbill_id::text;

-- 4) Replace old key columns.
ALTER TABLE fiscalbill DROP CONSTRAINT IF EXISTS fiscalbill_pkey;

ALTER TABLE fiscalbillpay DROP COLUMN IF EXISTS fiscalbill_id;
ALTER TABLE fiscalbillpay RENAME COLUMN fiscalbill_id_new TO fiscalbill_id;
ALTER TABLE fiscalbillpay ALTER COLUMN fiscalbill_id SET NOT NULL;

ALTER TABLE fiscalbillline DROP COLUMN IF EXISTS fiscalbill_id;
ALTER TABLE fiscalbillline RENAME COLUMN fiscalbill_id_new TO fiscalbill_id;
ALTER TABLE fiscalbillline ALTER COLUMN fiscalbill_id SET NOT NULL;

ALTER TABLE fiscalbilltax DROP COLUMN IF EXISTS fiscalbill_id;
ALTER TABLE fiscalbilltax RENAME COLUMN fiscalbill_id_new TO fiscalbill_id;

ALTER TABLE fiscalbill_idempotency_keys DROP COLUMN IF EXISTS fiscalbill_id;
ALTER TABLE fiscalbill_idempotency_keys RENAME COLUMN fiscalbill_id_new TO fiscalbill_id;
ALTER TABLE fiscalbill_idempotency_keys ALTER COLUMN fiscalbill_id SET NOT NULL;

ALTER TABLE fiscalbill DROP COLUMN IF EXISTS fiscalbill_id;
ALTER TABLE fiscalbill RENAME COLUMN fiscalbill_id_new TO fiscalbill_id;
ALTER TABLE fiscalbill ALTER COLUMN fiscalbill_id SET NOT NULL;
ALTER TABLE fiscalbill ALTER COLUMN fiscalbill_id SET DEFAULT nextval('fiscalbill_id_seq');
ALTER TABLE fiscalbill ADD CONSTRAINT fiscalbill_pkey PRIMARY KEY (fiscalbill_id);

-- 5) Recreate FK constraints.
ALTER TABLE fiscalbill_idempotency_keys
    ADD CONSTRAINT fk_fiscalbill_idempotency_bill
        FOREIGN KEY (fiscalbill_id)
        REFERENCES fiscalbill(fiscalbill_id);

ALTER TABLE fiscalbillpay
    ADD CONSTRAINT fk_fiscalbillpay_bill
        FOREIGN KEY (fiscalbill_id)
        REFERENCES fiscalbill(fiscalbill_id);

ALTER TABLE fiscalbillline
    ADD CONSTRAINT fk_fiscalbillline_bill
        FOREIGN KEY (fiscalbill_id)
        REFERENCES fiscalbill(fiscalbill_id);

ALTER TABLE fiscalbilltax
    ADD CONSTRAINT fk_fiscalbilltax_bill
        FOREIGN KEY (fiscalbill_id)
        REFERENCES fiscalbill(fiscalbill_id);

-- 6) Recreate indexes that may be dropped with old columns.
CREATE INDEX IF NOT EXISTS idx_fiscalbillpay_bill ON fiscalbillpay (fiscalbill_id);
CREATE INDEX IF NOT EXISTS idx_fiscalbillline_bill ON fiscalbillline (fiscalbill_id);
