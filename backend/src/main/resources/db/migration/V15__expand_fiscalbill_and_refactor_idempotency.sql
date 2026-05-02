ALTER TABLE fiscalbill
    RENAME COLUMN fiscal_document_id TO fiscalbill_id;

ALTER TABLE fiscalbill
    ADD COLUMN IF NOT EXISTS client_id NUMERIC(10,0) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS org_id NUMERIC(10,0) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS createdby NUMERIC(10,0) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS efiscal_address VARCHAR(50),
    ADD COLUMN IF NOT EXISTS efiscal_businessname VARCHAR(100),
    ADD COLUMN IF NOT EXISTS efiscal_code VARCHAR(1),
    ADD COLUMN IF NOT EXISTS efiscal_encryptedinternaldata TEXT,
    ADD COLUMN IF NOT EXISTS efiscal_invoicecounter VARCHAR(22),
    ADD COLUMN IF NOT EXISTS efiscal_invoicecounterext VARCHAR(22),
    ADD COLUMN IF NOT EXISTS efiscal_link VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS efiscal_messages VARCHAR(22),
    ADD COLUMN IF NOT EXISTS efiscal_mrc VARCHAR(22),
    ADD COLUMN IF NOT EXISTS efiscal_name VARCHAR(50),
    ADD COLUMN IF NOT EXISTS efiscal_qr TEXT,
    ADD COLUMN IF NOT EXISTS efiscal_requestedby VARCHAR(50),
    ADD COLUMN IF NOT EXISTS efiscal_sdcdatetime VARCHAR(50),
    ADD COLUMN IF NOT EXISTS efiscal_sdc_invoiceno VARCHAR(30),
    ADD COLUMN IF NOT EXISTS efiscal_signature TEXT,
    ADD COLUMN IF NOT EXISTS efiscal_signedby VARCHAR(22),
    ADD COLUMN IF NOT EXISTS efiscal_taxgrouprevision NUMERIC(10,0),
    ADD COLUMN IF NOT EXISTS efiscal_tin VARCHAR(22),
    ADD COLUMN IF NOT EXISTS efiscal_totalamount NUMERIC,
    ADD COLUMN IF NOT EXISTS efiscal_totalcounter NUMERIC,
    ADD COLUMN IF NOT EXISTS efiscal_transactiontypecounter NUMERIC(10,0),
    ADD COLUMN IF NOT EXISTS efiscal_type VARCHAR(2),
    ADD COLUMN IF NOT EXISTS fiscalbill_uu VARCHAR(36),
    ADD COLUMN IF NOT EXISTS isactive CHAR(1) NOT NULL DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS processed CHAR(1) NOT NULL DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS processedon NUMERIC,
    ADD COLUMN IF NOT EXISTS updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updatedby NUMERIC(10,0) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS value VARCHAR(40),
    ADD COLUMN IF NOT EXISTS efiscal_invoicetype NUMERIC(10,0),
    ADD COLUMN IF NOT EXISTS efiscal_transactiontype NUMERIC(10,0),
    ADD COLUMN IF NOT EXISTS efiscal_customername VARCHAR(100),
    ADD COLUMN IF NOT EXISTS efiscal_orderid VARCHAR(22);

ALTER TABLE fiscal_bill_idempotency_keys
    DROP CONSTRAINT IF EXISTS fk_fiscal_idempotency_bill;

ALTER TABLE fiscal_bill_idempotency_keys
    RENAME TO fiscalbill_idempotency_keys;

ALTER TABLE fiscalbill_idempotency_keys
    RENAME COLUMN fiscal_document_id TO fiscalbill_id;

ALTER TABLE fiscalbill_idempotency_keys
    ADD CONSTRAINT fk_fiscalbill_idempotency_bill
        FOREIGN KEY (fiscalbill_id)
        REFERENCES fiscalbill(fiscalbill_id);

CREATE TABLE IF NOT EXISTS fiscalbilltax
(
    fiscalbilltax_id BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000) PRIMARY KEY,
    client_id NUMERIC(10,0) NOT NULL,
    org_id NUMERIC(10,0) NOT NULL,
    amount NUMERIC,
    efiscal_categoryname VARCHAR(60),
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    createdby NUMERIC(10,0) NOT NULL,
    efiscal_taxlabel VARCHAR(1),
    fiscalbill_id VARCHAR(64),
    fiscalbilltax_uu VARCHAR(36),
    isactive CHAR(1) NOT NULL DEFAULT 'Y',
    processed CHAR(1) NOT NULL DEFAULT 'N',
    processedon NUMERIC,
    rate NUMERIC,
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updatedby NUMERIC(10,0) NOT NULL,
    value VARCHAR(40),
    efiscal_categorytype NUMERIC(10,0),
    CONSTRAINT fk_fiscalbilltax_bill FOREIGN KEY (fiscalbill_id) REFERENCES fiscalbill (fiscalbill_id)
);
