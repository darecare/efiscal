-- Role table
CREATE TABLE role (
    role_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code   VARCHAR(100) NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_role_code UNIQUE (role_code)
);

-- Client table
CREATE TABLE client (
    client_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    currency   VARCHAR(10) NOT NULL DEFAULT 'RSD',
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- Organization table
CREATE TABLE org (
    org_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id  UUID NOT NULL,
    name       VARCHAR(255) NOT NULL,
    tax_id     VARCHAR(50),
    status     VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    currency   VARCHAR(10) NOT NULL DEFAULT 'RSD',
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_org_client FOREIGN KEY (client_id) REFERENCES client (client_id)
);

CREATE INDEX idx_org_client_id ON org (client_id);

-- Users table  (not named "user" to avoid reserved-word conflicts)
CREATE TABLE users (
    user_id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id                UUID NOT NULL,
    email                    VARCHAR(255) NOT NULL,
    password_hash            VARCHAR(255) NOT NULL,
    full_name                VARCHAR(255) NOT NULL,
    role_id                  UUID NOT NULL,
    subscription_status      VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    subscription_start_at    TIMESTAMPTZ,
    subscription_expires_at  TIMESTAMPTZ,
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at               TIMESTAMPTZ,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_client FOREIGN KEY (client_id) REFERENCES client (client_id),
    CONSTRAINT fk_users_role   FOREIGN KEY (role_id)   REFERENCES role   (role_id)
);

CREATE INDEX idx_users_email     ON users (email);
CREATE INDEX idx_users_client_id ON users (client_id);

-- User → Organization access mapping
CREATE TABLE user_orgaccess (
    user_id    UUID NOT NULL,
    org_id     UUID NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, org_id),
    CONSTRAINT fk_uoa_user FOREIGN KEY (user_id) REFERENCES users  (user_id),
    CONSTRAINT fk_uoa_org  FOREIGN KEY (org_id)  REFERENCES org    (org_id)
);
