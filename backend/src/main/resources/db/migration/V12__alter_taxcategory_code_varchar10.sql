ALTER TABLE taxcategory
    ALTER COLUMN taxcategory_code TYPE VARCHAR(10) USING TRIM(taxcategory_code);
