ALTER TABLE taxcategory
    ALTER COLUMN taxcategory_code TYPE CHAR(10) USING taxcategory_code::TEXT::CHAR(10);
