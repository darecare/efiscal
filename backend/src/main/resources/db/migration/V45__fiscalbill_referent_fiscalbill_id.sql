-- Persist link to the fiscal bill used as Tax Authority referent document.
ALTER TABLE fiscalbill
    ADD COLUMN IF NOT EXISTS referent_fiscalbill_id BIGINT NULL;

ALTER TABLE fiscalbill
    DROP CONSTRAINT IF EXISTS fk_fiscalbill_referent;

ALTER TABLE fiscalbill
    ADD CONSTRAINT fk_fiscalbill_referent
        FOREIGN KEY (referent_fiscalbill_id)
        REFERENCES fiscalbill (fiscalbill_id);

CREATE INDEX IF NOT EXISTS idx_fiscalbill_referent
    ON fiscalbill (referent_fiscalbill_id);
