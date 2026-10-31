package com.ledgerline.ledger;

import com.ledgerline.DomainException;

/** A user's account would go below zero. User accounts can't be overdrawn. */
public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException(Money available) {
        super(
                Kind.RULE_VIOLATION,
                "Not enough money",
                "This wallet has " + available.toPlainString() + " " + available.currency()
                        + " available, which isn't enough.",
                null);
    }
}
