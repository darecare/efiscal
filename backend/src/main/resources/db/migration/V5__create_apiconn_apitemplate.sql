-- API Connections table
CREATE TABLE apiconn (
    apiconn_id    BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000 INCREMENT BY 1) PRIMARY KEY,
    org_id        BIGINT NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    api_platform  VARCHAR(50) NOT NULL,
    api_base_url  VARCHAR(500),
    apiauthtype   VARCHAR(50),
    apikey        VARCHAR(255),
    apisecret     VARCHAR(255),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT fk_apiconn_org FOREIGN KEY (org_id) REFERENCES org (org_id)
);

CREATE INDEX idx_apiconn_org_id ON apiconn (org_id);

-- API Templates table
CREATE TABLE apitemplate (
    apitemplate_id BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000 INCREMENT BY 1) PRIMARY KEY,
    apiconn_id     BIGINT NOT NULL,
    operation_key  VARCHAR(120) NOT NULL,
    http_method    VARCHAR(16) NOT NULL,
    content_type   VARCHAR(100) NOT NULL DEFAULT 'application/json',
    endpoint_path  VARCHAR(500) NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_apitemplate_conn FOREIGN KEY (apiconn_id) REFERENCES apiconn (apiconn_id),
    CONSTRAINT uq_apitemplate_conn_op UNIQUE (apiconn_id, operation_key)
);

CREATE INDEX idx_apitemplate_apiconn_id ON apitemplate (apiconn_id);
