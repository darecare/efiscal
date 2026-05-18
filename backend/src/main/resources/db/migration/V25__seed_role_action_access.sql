-- Seed built-in roles (idempotent) so role_action_access can reference them on first Flyway run.
INSERT INTO role (role_code, name, description, is_active, client_id)
SELECT 'SUPERADMIN', 'Super Administrator', 'Full unrestricted access across all clients and organizations', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'SUPERADMIN' AND client_id IS NULL);

INSERT INTO role (role_code, name, description, is_active, client_id)
SELECT 'CLIENT_ADMIN', 'Client Administrator', 'Administrative access within an assigned client scope', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'CLIENT_ADMIN' AND client_id IS NULL);

INSERT INTO role (role_code, name, description, is_active, client_id)
SELECT 'OPERATOR', 'Operator', 'Standard operational access for day-to-day tasks', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'OPERATOR' AND client_id IS NULL);

-- Seed role_action_access for built-in roles (idempotent)
INSERT INTO role_action_access (role_id, action_id, is_allowed)
SELECT r.role_id, a.action_id, TRUE
FROM role r
CROSS JOIN action_catalog a
WHERE r.role_code = 'SUPERADMIN' AND r.client_id IS NULL
  AND a.is_active = TRUE
ON CONFLICT (role_id, action_id) DO NOTHING;

INSERT INTO role_action_access (role_id, action_id, is_allowed)
SELECT r.role_id, a.action_id, TRUE
FROM role r
JOIN action_catalog a ON a.action_code IN (
    'FISCAL_CREATE_BILL',
    'FISCAL_RETRY_BILL',
    'FISCAL_VIEW_BILLS',
    'MERCHANTPRO_FETCH_ORDERS',
    'USERS_MANAGE'
)
WHERE r.role_code = 'CLIENT_ADMIN' AND r.client_id IS NULL
  AND a.is_active = TRUE
ON CONFLICT (role_id, action_id) DO NOTHING;

INSERT INTO role_action_access (role_id, action_id, is_allowed)
SELECT r.role_id, a.action_id, TRUE
FROM role r
JOIN action_catalog a ON a.action_code IN (
    'FISCAL_VIEW_BILLS',
    'MERCHANTPRO_FETCH_ORDERS'
)
WHERE r.role_code = 'OPERATOR' AND r.client_id IS NULL
  AND a.is_active = TRUE
ON CONFLICT (role_id, action_id) DO NOTHING;
