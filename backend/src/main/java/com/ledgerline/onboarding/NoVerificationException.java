package com.ledgerline.onboarding;

import com.ledgerline.DomainException;

class NoVerificationException extends DomainException {

    NoVerificationException() {
        super(Kind.NOT_FOUND, "Verification not found", "This user hasn't submitted a document.", null);
    }
}
