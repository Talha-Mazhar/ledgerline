-- Roles: customers use their own wallets; support staff work the back office; admins can also
-- change roles. Existing users are customers.
ALTER TABLE users
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'CUSTOMER'
        CHECK (role IN ('CUSTOMER', 'SUPPORT', 'ADMIN'));

-- Two staff accounts (password "ledgerline-demo"), so a held payment can be approved by two
-- different people, as the four-eyes rule requires.
INSERT INTO users (id, email, display_name, password_hash, created_at, version, role)
VALUES ('00000000-0000-4000-8000-000000000003', 'support@ledgerline.dev', 'Sam Support',
        '{bcrypt}$2a$10$y/kmpeipmgLDq9rKbQxUmu7y30IcaNJpjCtT4O01shp/sZkMX8lQa', now(), 0, 'SUPPORT'),
       ('00000000-0000-4000-8000-000000000004', 'admin@ledgerline.dev', 'Alex Admin',
        '{bcrypt}$2a$10$y/kmpeipmgLDq9rKbQxUmu7y30IcaNJpjCtT4O01shp/sZkMX8lQa', now(), 0, 'ADMIN');
