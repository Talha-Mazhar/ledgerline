-- The double-entry ledger. Every movement of money is one journal entry with two or more postings,
-- and the postings of an entry add up to zero in each currency: money never appears or vanishes,
-- it only moves between accounts.
--
-- The ledger is append-only. Postings and entries are never updated or deleted; a mistake is
-- corrected by a new, reversing entry. Triggers at the end of this file enforce both rules in the
-- database itself, so even a hand-written SQL script can't break them.

CREATE TABLE ledger_accounts
(
    id         UUID        NOT NULL PRIMARY KEY,
    kind       VARCHAR(16) NOT NULL CHECK (kind IN ('USER', 'FUNDING', 'PAYOUT', 'FX')),
    owner_id   UUID,
    currency   VARCHAR(3)  NOT NULL CHECK (currency IN ('EUR', 'USD', 'CZK', 'GBP')),
    created_at TIMESTAMPTZ NOT NULL,
    -- A user's account has an owner; Ledgerline's own (system) accounts have none.
    CONSTRAINT ck_ledger_accounts_owner CHECK ((kind = 'USER') = (owner_id IS NOT NULL)),
    -- Lets postings reference (account, currency) together, so a posting can't use the wrong currency.
    CONSTRAINT uq_ledger_accounts_id_currency UNIQUE (id, currency)
);

-- One system account of each kind per currency.
CREATE UNIQUE INDEX uq_ledger_system_accounts ON ledger_accounts (kind, currency) WHERE owner_id IS NULL;

CREATE TABLE journal_entries
(
    id                  UUID         NOT NULL PRIMARY KEY,
    kind                VARCHAR(16)  NOT NULL CHECK (kind IN ('DEPOSIT', 'WITHDRAWAL', 'EXCHANGE', 'TRANSFER')),
    initiated_by        UUID         NOT NULL,
    idempotency_key     VARCHAR(64)  NOT NULL,
    request_fingerprint VARCHAR(256) NOT NULL,
    posted_at           TIMESTAMPTZ  NOT NULL,
    -- A client's retry with the same key finds the first entry instead of creating a second one.
    CONSTRAINT uq_journal_entries_idempotency UNIQUE (initiated_by, idempotency_key)
);

CREATE TABLE postings
(
    id            UUID          NOT NULL PRIMARY KEY,
    -- Global insertion order: the order lines are shown in, and how "latest balance" is found.
    seq           BIGINT        GENERATED ALWAYS AS IDENTITY UNIQUE,
    entry_id      UUID          NOT NULL REFERENCES journal_entries (id),
    account_id    UUID          NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL CHECK (amount <> 0),
    currency      VARCHAR(3)    NOT NULL,
    -- The account's balance right after this posting: a statement line needs no recalculation, and
    -- an account's balance is its latest posting's value.
    balance_after NUMERIC(19, 2) NOT NULL,
    memo          VARCHAR(200)  NOT NULL,
    CONSTRAINT fk_postings_account FOREIGN KEY (account_id, currency) REFERENCES ledger_accounts (id, currency)
);

CREATE INDEX ix_postings_account_seq ON postings (account_id, seq DESC);
CREATE INDEX ix_postings_entry ON postings (entry_id);

-- Ledgerline's own accounts: where deposited money comes from, where withdrawn money goes, and the
-- pool that buys and sells currencies.
INSERT INTO ledger_accounts (id, kind, owner_id, currency, created_at)
SELECT gen_random_uuid(), kind, NULL, currency, now()
FROM (VALUES ('FUNDING'), ('PAYOUT'), ('FX')) AS kinds (kind)
         CROSS JOIN (VALUES ('EUR'), ('USD'), ('CZK'), ('GBP')) AS currencies (currency);

-- Wallets opened before the ledger existed get their account now. A wallet's ledger account has the
-- same id as the wallet.
INSERT INTO ledger_accounts (id, kind, owner_id, currency, created_at)
SELECT id, 'USER', owner_id, currency, opened_at
FROM wallets;

-- Every entry must balance in every currency. A deferred constraint trigger checks at commit, after
-- all of the entry's postings have been inserted.
CREATE FUNCTION ledger_check_entry_balanced() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF EXISTS (SELECT 1
               FROM postings
               WHERE entry_id = NEW.entry_id
               GROUP BY currency
               HAVING SUM(amount) <> 0) THEN
        RAISE EXCEPTION 'journal entry % does not balance', NEW.entry_id;
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_postings_balanced
    AFTER INSERT
    ON postings
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION ledger_check_entry_balanced();

-- The ledger is append-only.
CREATE FUNCTION ledger_forbid_change() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION '% is append-only: record a new entry instead', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_postings_append_only
    BEFORE UPDATE OR DELETE
    ON postings
    FOR EACH ROW
EXECUTE FUNCTION ledger_forbid_change();

CREATE TRIGGER trg_journal_entries_append_only
    BEFORE UPDATE OR DELETE
    ON journal_entries
    FOR EACH ROW
EXECUTE FUNCTION ledger_forbid_change();
