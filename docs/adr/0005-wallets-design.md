# 0005. Wallets: ownership, balances and identity before sign-in

- Status: accepted
- Date: 2026-10-30

## Context

The first feature is wallets: a user opens a wallet in EUR, USD, CZK or GBP and
sees its balance. Three questions come up before any money moves.

## 1. Where does the balance live?

**Option A: a `balance` column on the wallet**, updated on every payment.
Fast to read, but it is a second copy of the truth. A bug, a failed
transaction or a manual fix can change one copy and not the other, and then
nobody knows which is right.

**Option B: derive it from the ledger** (chosen). The wallet has no balance
column; the balance is the sum of the wallet account's postings in the ledger.
There is one record of money, and every balance can be explained entry by
entry. When reads get expensive, the ledger can keep a running balance per
account, but the ledger stays the owner. Until the ledger records postings
(next milestone), `WalletService.balanceOf` returns zero, which is the true
balance of a wallet that has never received money.

## 2. How is "one wallet per currency" enforced?

The service checks first, which gives a clear error in the normal case. Two
requests at the same moment can both pass that check, so the real guarantee is
the database's unique constraint on `(owner_id, currency)`. `saveAndFlush`
makes the constraint fire inside the service method, where it is turned into
the same "already open" error. Rule of thumb: the application gives good
errors, the database gives guarantees.

## 3. Whose wallets, before sign-in exists?

Every query includes the owner, so a user can never read another user's wallet
by guessing its id; it is simply not found (404, not 403, which would confirm
the wallet exists).

The owner comes from `identity.CurrentUser`, an interface. Until sign-in is
built, `DemoCurrentUser` answers with one fixed demo user. When Spring Security
arrives, a new implementation reads the signed-in user, and the wallets module
doesn't change. This is the dependency inversion principle applied across
modules: wallets depend on what identity promises, not on how it works.

## Other choices

- **Ids are UUIDs created in Java**, not database sequences: an entity has its
  identity before it is saved, ids don't reveal how many wallets exist, and
  they can't be guessed in order.
- **Optimistic locking** (`@Version`) on wallets, so two concurrent updates
  can't silently overwrite each other once wallets can change (freezing,
  closing).
- **Time comes from an injected `Clock`** in UTC, so tests can fix it.
- **Everything in the wallets module is package-private** except what other
  modules will need. The compiler stops other modules from reaching into wallet
  internals; Spring Modulith's test checks the module dependencies.
