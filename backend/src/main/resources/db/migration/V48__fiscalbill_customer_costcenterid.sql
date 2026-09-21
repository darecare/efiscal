-- Persist Tax Authority buyerCostCenterId (optional customer field) on the fiscal bill.
ALTER TABLE fiscalbill
    ADD COLUMN IF NOT EXISTS customer_costcenterid VARCHAR(128);
