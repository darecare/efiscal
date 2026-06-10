INSERT INTO action_catalog (module_code, action_code, name, description)
SELECT 'FISCAL', 'FISCAL_MANAGE_PRODUCTS', 'Manage Products', 'Allows managing product catalog and syncing from shop'
WHERE NOT EXISTS (
    SELECT 1 FROM action_catalog WHERE module_code = 'FISCAL' AND action_code = 'FISCAL_MANAGE_PRODUCTS'
);

INSERT INTO role_action_access (role_id, action_id, is_allowed)
SELECT r.role_id, a.action_id, TRUE
FROM role r
JOIN action_catalog a ON a.action_code = 'FISCAL_MANAGE_PRODUCTS' AND a.is_active = TRUE
WHERE r.role_code = 'SUPERADMIN' AND r.client_id IS NULL
ON CONFLICT (role_id, action_id) DO NOTHING;

INSERT INTO role_action_access (role_id, action_id, is_allowed)
SELECT r.role_id, a.action_id, TRUE
FROM role r
JOIN action_catalog a ON a.action_code = 'FISCAL_MANAGE_PRODUCTS' AND a.is_active = TRUE
WHERE r.role_code = 'CLIENT_ADMIN' AND r.client_id IS NULL
ON CONFLICT (role_id, action_id) DO NOTHING;
