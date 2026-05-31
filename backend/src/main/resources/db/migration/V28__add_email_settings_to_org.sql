ALTER TABLE org
    ADD COLUMN smtp_server VARCHAR(255),
    ADD COLUMN smtp_port INTEGER,
    ADD COLUMN email_from VARCHAR(255),
    ADD COLUMN smtp_username VARCHAR(255),
    ADD COLUMN smtp_password VARCHAR(255),
    ADD COLUMN smtp_connection_security VARCHAR(20);
