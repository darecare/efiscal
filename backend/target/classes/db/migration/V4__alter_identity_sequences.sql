-- Change all identity sequences to start at 1000 (4-digit minimum, grows to 10+ digits)
ALTER TABLE role   ALTER COLUMN role_id   RESTART WITH 1000;
ALTER TABLE client ALTER COLUMN client_id RESTART WITH 1000;
ALTER TABLE org    ALTER COLUMN org_id    RESTART WITH 1000;
ALTER TABLE users  ALTER COLUMN user_id   RESTART WITH 1000;
