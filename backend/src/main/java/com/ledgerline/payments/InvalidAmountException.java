package com.ledgerline.payments;

import com.ledgerline.DomainException;

/** The amount isn't a positive number with at most two decimal places. */
class InvalidAmountException extends DomainException {

    InvalidAmountException(String detail) {
        super(Kind.INVALID_REQUEST, "Invalid amount", detail, null);
    }
}
