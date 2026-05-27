ALTER TABLE system_accounts DROP CONSTRAINT IF EXISTS system_accounts_username_key;
ALTER TABLE system_accounts DROP CONSTRAINT IF EXISTS system_accounts_email_key;

DROP INDEX IF EXISTS uk_system_accounts_username_active;
DROP INDEX IF EXISTS uk_system_accounts_email_active;

CREATE UNIQUE INDEX uk_system_accounts_username_active
ON system_accounts (username)
WHERE is_deleted = false;

CREATE UNIQUE INDEX uk_system_accounts_email_active
ON system_accounts (email)
WHERE is_deleted = false;
