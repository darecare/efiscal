-- 1. Add client_id to role table
ALTER TABLE role ADD COLUMN client_id BIGINT;
ALTER TABLE role ADD CONSTRAINT fk_role_client FOREIGN KEY (client_id) REFERENCES client(client_id);
-- Because roles can be global (SUPERADMIN), client_id is nullable.

-- Drop existing unique constraint on role_code
ALTER TABLE role DROP CONSTRAINT uq_role_code;

-- Create new unique constraint: (client_id, role_code).
-- Coalesce treats NULL client_id as 0 for uniqueness
CREATE UNIQUE INDEX uq_role_client_code ON role (role_code, COALESCE(client_id, 0));

-- 2. Create action_catalog table
CREATE TABLE action_catalog (
    action_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    module_code    VARCHAR(80) NOT NULL,
    action_code    VARCHAR(120) NOT NULL,
    name           VARCHAR(120) NOT NULL,
    description    VARCHAR(255),
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_action_catalog_code UNIQUE (module_code, action_code)
);

-- 3. Create role_action_access table
CREATE TABLE role_action_access (
    role_id        BIGINT NOT NULL,
    action_id      BIGINT NOT NULL,
    is_allowed     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_id, action_id),
    CONSTRAINT fk_raa_role FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_raa_action FOREIGN KEY (action_id) REFERENCES action_catalog(action_id) ON DELETE CASCADE
);

-- 4. Seed action_catalog
INSERT INTO action_catalog (module_code, action_code, name, description) VALUES
('FISCAL', 'FISCAL_CREATE_BILL', 'Create Fiscal Bill', 'Allows issuing new fiscal bills'),
('FISCAL', 'FISCAL_RETRY_BILL', 'Retry Fiscal Bill', 'Allows retrying failed fiscal bills'),
('FISCAL', 'FISCAL_VIEW_BILLS', 'View Fiscal Bills', 'Allows viewing fiscal bills list and details'),
('MERCHANTPRO', 'MERCHANTPRO_FETCH_ORDERS', 'Fetch Orders', 'Allows fetching orders from MerchantPro API'),
('SYSTEM', 'USERS_MANAGE', 'Manage Users', 'Allows creating and editing users'),
('SYSTEM', 'ROLES_MANAGE', 'Manage Roles', 'Allows creating and editing roles'),
('SYSTEM', 'ORGS_MANAGE', 'Manage Organizations', 'Allows managing organizations and API settings');
