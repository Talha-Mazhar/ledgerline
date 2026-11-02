-- A payment is what a user asked for (add money, withdraw, exchange, send) and where it stands.
-- The ledger records the money; this table records the request and its state machine:
--
--   COMPLETED                                   the money moved at once
--   HELD -> AWAITING_SECOND_APPROVAL -> COMPLETED  over a limit: two different staff approve
--   HELD | AWAITING_SECOND_APPROVAL -> REJECTED  staff said no; the money went back
--   COMPLETED -> REFUNDED                        staff reversed it

CREATE TABLE payments
(
    id                  UUID           NOT NULL PRIMARY KEY,
    kind                VARCHAR(16)    NOT NULL CHECK (kind IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'EXCHANGE', 'REFUND')),
    status              VARCHAR(32)    NOT NULL
        CHECK (status IN ('COMPLETED', 'HELD', 'AWAITING_SECOND_APPROVAL', 'REJECTED', 'REFUNDED')),
    initiated_by        UUID           NOT NULL,
    idempotency_key     VARCHAR(64)    NOT NULL,
    request_fingerprint VARCHAR(256)   NOT NULL,
    from_wallet_id      UUID,
    to_wallet_id        UUID,
    counterparty_id     UUID,
    amount              NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3)     NOT NULL,
    entry_id            UUID           NOT NULL,
    hold_reason         VARCHAR(500),
    refund_of           UUID REFERENCES payments (id),
    first_approver      UUID,
    second_approver     UUID,
    rejected_by         UUID,
    rejection_reason    VARCHAR(500),
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    version             BIGINT         NOT NULL,
    CONSTRAINT uq_payments_idempotency UNIQUE (initiated_by, idempotency_key),
    -- The four-eyes rule, in the database as well as the code.
    CONSTRAINT ck_payments_two_approvers CHECK (second_approver IS NULL OR second_approver <> first_approver)
);

CREATE INDEX ix_payments_status ON payments (status, created_at);
CREATE INDEX ix_payments_initiator ON payments (initiated_by, created_at DESC);
CREATE INDEX ix_payments_counterparty ON payments (counterparty_id, created_at DESC);
