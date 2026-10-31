package com.ledgerline.ledger;

import com.ledgerline.DomainException;

/** A client reused an Idempotency-Key for a request that differs from the first one. */
public class IdempotencyKeyReusedException extends DomainException {

    public IdempotencyKeyReusedException() {
        super(
                Kind.CONFLICT,
                "Idempotency key reused",
                "This Idempotency-Key was already used for a different request. Send a new key.",
                null);
    }
}
