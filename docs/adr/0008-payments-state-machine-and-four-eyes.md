# 0008. Payments as a state machine, with four-eyes approval of held payments

- Status: accepted
- Date: 2026-11-01

## Context

Until now every movement of money went straight into the ledger. A real payments product can't
do that: a basic customer sending €1,500 to someone they never paid before should wait for a
person to look at it, and staff need to reverse mistakes. Money set aside while waiting must not
be spendable, and the books must still balance at every moment.

## Options

**A. A pending flag on the ledger entry.** Simple, but the ledger stops being append-only: an
entry would change state after it was written, and balances would depend on flags.

**B. A payment record with a state machine, and ledger entries for each step** (chosen). The
payment (`payments` table) holds the business state; the ledger only ever receives complete,
balanced entries. Holding money is itself an entry: from the customer's wallet to a per-currency
HOLD account.

## Decision

- `PaymentStatus`: `COMPLETED`, `HELD`, `AWAITING_SECOND_APPROVAL`, `REJECTED`, `REFUNDED`.
  `canMoveTo` lists the allowed transitions; anything else is refused before touching money.
- A held transfer posts *customer → HOLD*. Approval posts *HOLD → recipient*; rejection posts
  *HOLD → customer*. A refund posts the reverse of the original entry. Each has an idempotency key
  derived from the payment (`payment-<id>-settle`, `-release`, `refund-<id>`), so a retried
  approval can't move money twice.
- **Four eyes.** Releasing a held payment takes two approvals by two different members of staff.
  The service refuses a second approval by the first approver, and a database `CHECK` constraint
  (`ck_payments_two_approvers`) refuses it even if a bug got past the service.
- The payment row is locked (`SELECT … FOR UPDATE`) while it changes state, so two staff clicking
  at once can't both approve or approve and reject.
- Risk rules (`PaymentScreening`) decide whether to hold: per-tier single-payment and daily limits,
  a velocity rule (more than five payments in ten minutes) and a first large payment to a new
  recipient. The reasons are stored and shown to the customer and to staff.
- `Idempotency-Key` on the request is unique per user, enforced by a unique index, so a retried
  request returns the first result.

## Consequences

The customer's balance drops as soon as money is held, which is what they expect: it is no longer
theirs to spend. The ledger stays append-only and sums to zero per currency, which a concurrency
test checks with 100 parallel transfers.
