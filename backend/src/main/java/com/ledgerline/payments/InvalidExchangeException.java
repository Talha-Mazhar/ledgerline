package com.ledgerline.payments;

import com.ledgerline.DomainException;

/** An exchange needs two different wallets in two different currencies. */
class InvalidExchangeException extends DomainException {

    InvalidExchangeException(String detail) {
        super(Kind.INVALID_REQUEST, "Invalid exchange", detail, null);
    }
}
