package com.ledgerline.fx;

import com.ledgerline.DomainException;

/** An exchange needs two different currencies. */
public class SameCurrencyException extends DomainException {

    public SameCurrencyException() {
        super(Kind.INVALID_REQUEST, "Same currency", "Choose two different currencies.", null);
    }
}
