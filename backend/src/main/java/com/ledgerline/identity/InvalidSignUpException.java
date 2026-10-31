package com.ledgerline.identity;

import com.ledgerline.DomainException;

/** A sign-up form that can't become an account, e.g. a password that is too short. */
class InvalidSignUpException extends DomainException {

    InvalidSignUpException(String message) {
        super(Kind.INVALID_REQUEST, "Check your details", message, null);
    }
}
