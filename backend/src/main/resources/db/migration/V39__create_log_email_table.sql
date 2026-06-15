CREATE TABLE IF NOT EXISTS log_email (
    log_email_id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL REFERENCES org(org_id),
    fiscalbill_id BIGINT NULL REFERENCES fiscalbill(fiscalbill_id),
    order_id VARCHAR(64),
    recipient_email VARCHAR(255),
    template_name VARCHAR(120),
    subject VARCHAR(255),
    body_html TEXT,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_log_email_org_id ON log_email(org_id);
CREATE INDEX IF NOT EXISTS idx_log_email_fiscalbill_id ON log_email(fiscalbill_id);
