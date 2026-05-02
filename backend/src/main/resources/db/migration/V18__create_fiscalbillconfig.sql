-- V18: Create fiscalbillconfig table (org-level fiscal configuration including esirno for invoiceNumber)

CREATE TABLE IF NOT EXISTS fiscalbillconfig
(
    fiscalbillconfig_id  BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000) PRIMARY KEY,
    client_id            NUMERIC(10, 0) DEFAULT 0,
    org_id               NUMERIC(10, 0) DEFAULT 0,
    esirno               VARCHAR(22),
    istest               VARCHAR(1)     NOT NULL DEFAULT 'N',
    email_from           VARCHAR(60),
    email_bcc            VARCHAR(60),
    email_test           VARCHAR(60),
    fiscalbillconfig_uu  VARCHAR(36),
    isactive             VARCHAR(1)     NOT NULL DEFAULT 'Y',
    created              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    createdby            NUMERIC(10, 0) NOT NULL DEFAULT 0,
    updated              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updatedby            NUMERIC(10, 0) NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_fiscalbillconfig_org ON fiscalbillconfig (org_id) WHERE isactive = 'Y';
