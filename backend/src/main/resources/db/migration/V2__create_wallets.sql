-- Wallets: one per user and currency. The balance is not stored here; it is derived from the
-- ledger. The check constraints repeat the Java enums, so a bad value can't enter the table even
-- from a script that bypasses the application.

CREATE TABLE wallets
(
    id        UUID        NOT NULL PRIMARY KEY,
    owner_id  UUID        NOT NULL,
    currency  VARCHAR(3)  NOT NULL CHECK (currency IN ('EUR', 'USD', 'CZK', 'GBP')),
    status    VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE')),
    opened_at TIMESTAMPTZ NOT NULL,
    version   BIGINT      NOT NULL,
    CONSTRAINT uq_wallets_owner_currency UNIQUE (owner_id, currency)
);

-- The unique constraint's index also serves "all wallets of this owner" lookups.
