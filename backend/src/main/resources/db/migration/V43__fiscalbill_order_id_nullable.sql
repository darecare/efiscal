-- Manual fiscal bill creation does not require an order (spec 4.2.1).
ALTER TABLE fiscalbill
    ALTER COLUMN order_id DROP NOT NULL;
