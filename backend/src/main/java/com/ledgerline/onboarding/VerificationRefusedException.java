package com.ledgerline.onboarding;

import com.ledgerline.DomainException;

class VerificationRefusedException extends DomainException {

    VerificationRefusedException(String message) {
        super(Kind.RULE_VIOLATION, "Verification can't be changed", message, null);
    }
}
