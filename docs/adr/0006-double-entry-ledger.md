# 0006. A double-entry ledger, written in SQL

- Status: accepted
- Date: 2026-10-31

## Context

Money moves in four ways: deposits, withdrawals, exchanges and transfers between users. Balances
must always be right, even when two requests for the same wallet arrive at the same moment or a
client retries a request whose answer it never received.

## Decision

**Every movement is one journal entry with two or more postings that add up to zero in each
currency.** A deposit of 100 EUR is `funding −100, wallet +100`; an exchange has four legs, two per
currency, through Ledgerline's FX accounts. Money is never created or lost, only moved, so the sum
of all postings per currency is always zero. That is a property anyone can check with one query.

**The ledger is append-only.** Postings and entries are never updated or deleted; a mistake is
fixed with a new, reversing entry. Triggers in PostgreSQL refuse `UPDATE` and `DELETE`, and a
deferred constraint trigger refuses, at commit, an entry that doesn't balance. The database keeps
these rules even against a hand-written script.

**Each posting stores the balance after it** (`balance_after`). A statement needs no
recalculation, and an account's balance is its latest posting's value.

**Concurrency: row locks in a fixed order.** Posting locks every account involved with
`SELECT ... FOR UPDATE`, sorted by id. Two payments from one wallet then run one after the other,
so they can't both spend the same money, and the fixed order means two payments can't wait on each
other forever. A test runs two withdrawals of 80 from 100 at once: exactly one succeeds.

**Idempotency: the client's key is a unique constraint.** Each movement carries an
`Idempotency-Key`. The entry is inserted with `ON CONFLICT DO NOTHING` on (user, key); a retry
finds the first entry and gets its result back. The same key with a different request (another
amount, another wallet) is refused with 409, detected by a fingerprint of the request.

**SQL (`JdbcClient`), not JPA, for the ledger.** The ledger is rows that never change, row locks
and `INSERT ... ON CONFLICT`, which SQL says directly. JPA stays where it helps: entities that
change over time, like wallets and users. Both share one transaction.

## Consequences

- Balances can be explained line by line, and corrections leave a trail.
- The ledger knows nothing about wallets or users: modules post entries through the `Ledger`
  interface, and the ledger module checks only that the entry balances and no user account goes
  below zero.
- Reads of "latest balance" use an index on `(account_id, seq DESC)`. If an account ever had
  millions of postings, a snapshot table would be the next step.
