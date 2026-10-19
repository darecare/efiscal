-- Rename customer name column and add customer email for fiscal bills.
ALTER TABLE fiscalbill
    RENAME COLUMN efiscal_customername TO customer_name;

ALTER TABLE fiscalbill
    ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255);
