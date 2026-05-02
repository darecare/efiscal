ALTER TABLE taxcategory
    RENAME COLUMN category_type TO taxcategory_code;

ALTER TABLE taxcategory
    ALTER COLUMN taxcategory_code TYPE CHAR(2) USING taxcategory_code::TEXT::CHAR(2);
