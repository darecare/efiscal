ALTER TABLE org ADD COLUMN advertisement_html    TEXT    NULL;
ALTER TABLE org ADD COLUMN advertisement_enabled BOOLEAN NOT NULL DEFAULT FALSE;
