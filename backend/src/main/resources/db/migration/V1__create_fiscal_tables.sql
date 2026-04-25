CREATE TABLE fiscal_bills (
    fiscal_document_id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(128),
    last_error VARCHAR(512),
    attempt_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_fiscal_bills_order_id ON fiscal_bills(order_id);

CREATE TABLE fiscal_bill_idempotency_keys (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    fiscal_document_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_fiscal_idempotency_bill
        FOREIGN KEY (fiscal_document_id)
        REFERENCES fiscal_bills(fiscal_document_id)
);
