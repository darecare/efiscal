-- Rename location column and add district from Tax Authority invoice response.
ALTER TABLE fiscalbill
    RENAME COLUMN efiscal_name TO efiscal_locationname;

ALTER TABLE fiscalbill
    ADD COLUMN IF NOT EXISTS efiscal_district VARCHAR(50);
