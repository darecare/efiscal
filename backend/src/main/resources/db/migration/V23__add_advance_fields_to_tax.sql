ALTER TABLE tax
    ADD COLUMN IF NOT EXISTS efiscal_advanceprefix VARCHAR(50),
    ADD COLUMN IF NOT EXISTS efiscal_advancename VARCHAR(50);
