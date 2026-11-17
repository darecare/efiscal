INSERT INTO action_catalog (module_code, action_code, name, description)
SELECT 'REPORTS', 'REPORTS_VIEW', 'View Reports', 'Allows opening Reports and running fiscal report exports'
WHERE NOT EXISTS (
    SELECT 1 FROM action_catalog WHERE module_code = 'REPORTS' AND action_code = 'REPORTS_VIEW'
);

INSERT INTO role_action_access (role_id, action_id, is_allowed)
SELECT r.role_id, a.action_id, TRUE
FROM role r
JOIN action_catalog a ON a.action_code = 'REPORTS_VIEW' AND a.is_active = TRUE
WHERE r.role_code = 'SUPERADMIN' AND r.client_id IS NULL
ON CONFLICT (role_id, action_id) DO NOTHING;

INSERT INTO role_action_access (role_id, action_id, is_allowed)
SELECT r.role_id, a.action_id, TRUE
FROM role r
JOIN action_catalog a ON a.action_code = 'REPORTS_VIEW' AND a.is_active = TRUE
WHERE r.role_code = 'CLIENT_ADMIN' AND r.client_id IS NULL
ON CONFLICT (role_id, action_id) DO NOTHING;

-- Supports Net Normal products-sold report: SUCCESS Normal bills by org + sdc datetime range
CREATE INDEX IF NOT EXISTS idx_fiscalbill_report_org_sdc
  ON fiscalbill (org_id, efiscal_sdcdatetime)
  WHERE status = 'SUCCESS' AND efiscal_invoicetype = 0;
