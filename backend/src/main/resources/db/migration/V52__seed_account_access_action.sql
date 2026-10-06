INSERT INTO action_catalog (module_code, action_code, name, description)
SELECT 'SYSTEM', 'ACCOUNT_ACCESS', 'Account Page', 'Allows opening the Account page and editing own name, email and password'
WHERE NOT EXISTS (
    SELECT 1 FROM action_catalog WHERE module_code = 'SYSTEM' AND action_code = 'ACCOUNT_ACCESS'
);

-- Every role could open the Account page before this permission existed; grant it once so no one loses access.
INSERT INTO role_action_access (role_id, action_id, is_allowed)
SELECT r.role_id, a.action_id, TRUE
FROM role r
JOIN action_catalog a ON a.action_code = 'ACCOUNT_ACCESS' AND a.is_active = TRUE
ON CONFLICT (role_id, action_id) DO NOTHING;
