-- V8__add_role_to_users.sql
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'ADMIN'));

UPDATE users
SET role = 'ADMIN'
WHERE identifier = 'admin@example.com'
  AND identifier_type = 'EMAIL';

CREATE INDEX idx_users_role
    ON users (role)
    WHERE role = 'ADMIN';