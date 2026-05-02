ALTER TABLE fiscalbill
    ADD COLUMN IF NOT EXISTS request_body TEXT;
