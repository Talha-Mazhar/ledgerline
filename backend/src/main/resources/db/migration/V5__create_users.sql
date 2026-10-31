-- Users who can sign in. Emails are stored in lower case, so "Jana@Example.com" and
-- "jana@example.com" are the same account; the unique constraint enforces it.
-- Passwords are stored only as bcrypt hashes, prefixed with the algorithm ("{bcrypt}") so the
-- hashing can be upgraded later without breaking existing passwords.

CREATE TABLE users
(
    id            UUID         NOT NULL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL CHECK (email = lower(email)),
    display_name  VARCHAR(80)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Two demo accounts, both with the password "ledgerline-demo", so the app can be tried at once.
-- The first keeps the id the app used before sign-in existed, so wallets opened then stay theirs.
INSERT INTO users (id, email, display_name, password_hash, created_at, version)
VALUES ('00000000-0000-4000-8000-000000000001', 'demo@ledgerline.dev', 'Demo user',
        '{bcrypt}$2a$10$y/kmpeipmgLDq9rKbQxUmu7y30IcaNJpjCtT4O01shp/sZkMX8lQa', now(), 0),
       ('00000000-0000-4000-8000-000000000002', 'jana@ledgerline.dev', 'Jana Nováková',
        '{bcrypt}$2a$10$y/kmpeipmgLDq9rKbQxUmu7y30IcaNJpjCtT4O01shp/sZkMX8lQa', now(), 0);

-- Jana starts with a euro and a koruna wallet, so the demo user has someone to send money to.
-- A wallet's ledger account has the wallet's id (see V3).
INSERT INTO wallets (id, owner_id, currency, status, opened_at, version)
VALUES ('00000000-0000-4000-8000-0000000a0001', '00000000-0000-4000-8000-000000000002', 'EUR', 'ACTIVE', now(), 0),
       ('00000000-0000-4000-8000-0000000a0002', '00000000-0000-4000-8000-000000000002', 'CZK', 'ACTIVE', now(), 0);

INSERT INTO ledger_accounts (id, kind, owner_id, currency, created_at)
SELECT id, 'USER', owner_id, currency, opened_at
FROM wallets
WHERE owner_id = '00000000-0000-4000-8000-000000000002';
