package com.ledgerline.onboarding;

import com.ledgerline.DomainException;

class InvalidDocumentException extends DomainException {

    InvalidDocumentException(String message) {
        super(Kind.INVALID_REQUEST, "Document not accepted", message, null);
    }
}
