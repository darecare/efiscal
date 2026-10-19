-- Persist Advance Sale line total paid (may be less than total_amount for partial advances).
ALTER TABLE fiscalbillline
    ADD COLUMN IF NOT EXISTS total_paid NUMERIC(14,2);
