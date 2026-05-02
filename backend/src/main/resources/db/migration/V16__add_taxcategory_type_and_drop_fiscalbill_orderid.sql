ALTER TABLE taxcategory
    ADD COLUMN IF NOT EXISTS efiscal_categorytype NUMERIC(10,0);

ALTER TABLE fiscalbill
    DROP COLUMN IF EXISTS efiscal_orderid;
