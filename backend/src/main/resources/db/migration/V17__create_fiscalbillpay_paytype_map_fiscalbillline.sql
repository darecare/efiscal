-- V17: Create fiscalbillpay, paytype_map, and fiscalbillline tables

-- fiscalbillpay: payment records per fiscal bill
CREATE TABLE IF NOT EXISTS fiscalbillpay
(
    fiscalbillpay_id BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000) PRIMARY KEY,
    fiscalbill_id    VARCHAR(64)    NOT NULL,
    client_id        NUMERIC(10, 0) NOT NULL DEFAULT 0,
    org_id           NUMERIC(10, 0) NOT NULL DEFAULT 0,
    payment_type     NUMERIC(10, 0) NOT NULL,
    amount           NUMERIC        NOT NULL,
    isactive         VARCHAR(1)     NOT NULL DEFAULT 'Y',
    created          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    createdby        NUMERIC(10, 0) NOT NULL DEFAULT 0,
    updated          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updatedby        NUMERIC(10, 0) NOT NULL DEFAULT 0,
    CONSTRAINT fk_fiscalbillpay_bill FOREIGN KEY (fiscalbill_id)
        REFERENCES fiscalbill (fiscalbill_id)
);

CREATE INDEX IF NOT EXISTS idx_fiscalbillpay_bill ON fiscalbillpay (fiscalbill_id);

-- paytype_map: maps external payment method codes to fiscal payment type integers, per client
CREATE TABLE IF NOT EXISTS paytype_map
(
    paytype_map_id       BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000) PRIMARY KEY,
    client_id            NUMERIC(10, 0) NOT NULL DEFAULT 0,
    payment_method_code  VARCHAR(50)    NOT NULL,
    payment_type         NUMERIC(10, 0) NOT NULL,
    description          VARCHAR(100),
    isactive             VARCHAR(1)     NOT NULL DEFAULT 'Y',
    created              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    createdby            NUMERIC(10, 0) NOT NULL DEFAULT 0,
    updated              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updatedby            NUMERIC(10, 0) NOT NULL DEFAULT 0,
    CONSTRAINT uq_paytype_map_client_code UNIQUE (client_id, payment_method_code)
);

-- fiscalbillline: line items for each fiscal bill
-- Used for manual fiscal bills; also populated after TA response for order-based fiscal bills
CREATE TABLE IF NOT EXISTS fiscalbillline
(
    fiscalbillline_id BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000) PRIMARY KEY,
    fiscalbill_id     VARCHAR(64)     NOT NULL,
    client_id         NUMERIC(10, 0)  NOT NULL DEFAULT 0,
    org_id            NUMERIC(10, 0)  NOT NULL DEFAULT 0,
    name              VARCHAR(2048)   NOT NULL,
    quantity          NUMERIC(14, 3)  NOT NULL,
    unit_price        NUMERIC(14, 2)  NOT NULL,
    total_amount      NUMERIC(14, 2)  NOT NULL,
    tax_label         VARCHAR(10),
    gtin              VARCHAR(14),
    product_id        VARCHAR(50),
    sku               VARCHAR(100),
    isactive          VARCHAR(1)      NOT NULL DEFAULT 'Y',
    created           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    createdby         NUMERIC(10, 0)  NOT NULL DEFAULT 0,
    updated           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updatedby         NUMERIC(10, 0)  NOT NULL DEFAULT 0,
    CONSTRAINT fk_fiscalbillline_bill FOREIGN KEY (fiscalbill_id)
        REFERENCES fiscalbill (fiscalbill_id)
);

CREATE INDEX IF NOT EXISTS idx_fiscalbillline_bill ON fiscalbillline (fiscalbill_id);
