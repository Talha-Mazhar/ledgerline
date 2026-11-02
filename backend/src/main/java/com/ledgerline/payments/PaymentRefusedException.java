package com.ledgerline.payments;

import com.ledgerline.DomainException;

/** A step the payment's state machine or the four-eyes rule doesn't allow. */
class PaymentRefusedException extends DomainException {

    PaymentRefusedException(String message) {
        super(Kind.RULE_VIOLATION, "Not allowed for this payment", message, null);
    }
}
