-- Persist Tax Authority buyerId (e.g. "10:123456789") on the fiscal bill.
ALTER TABLE fiscalbill
    ADD COLUMN IF NOT EXISTS customer_id VARCHAR(64);
