-- A payment that needs approval doesn't vanish into thin air while it waits: its money moves from
-- the wallet to Ledgerline's HOLD account in that currency. Approval moves it on to the recipient,
-- rejection back to the wallet. Every step is a balanced entry, so held money is always visible.

ALTER TABLE ledger_accounts DROP CONSTRAINT ledger_accounts_kind_check;
ALTER TABLE ledger_accounts
    ADD CONSTRAINT ledger_accounts_kind_check CHECK (kind IN ('USER', 'FUNDING', 'PAYOUT', 'FX', 'HOLD'));

INSERT INTO ledger_accounts (id, kind, owner_id, currency, created_at)
SELECT gen_random_uuid(), 'HOLD', NULL, currency, now()
FROM (VALUES ('EUR'), ('USD'), ('CZK'), ('GBP')) AS currencies (currency);

ALTER TABLE journal_entries DROP CONSTRAINT journal_entries_kind_check;
ALTER TABLE journal_entries
    ADD CONSTRAINT journal_entries_kind_check
        CHECK (kind IN ('DEPOSIT', 'WITHDRAWAL', 'EXCHANGE', 'TRANSFER', 'HOLD', 'RELEASE', 'REFUND'));
