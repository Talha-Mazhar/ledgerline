package com.ledgerline.payments;

import com.ledgerline.DomainException;

class PaymentNotFoundException extends DomainException {

    PaymentNotFoundException() {
        super(Kind.NOT_FOUND, "Payment not found", "There is no payment with this id.", null);
    }
}
