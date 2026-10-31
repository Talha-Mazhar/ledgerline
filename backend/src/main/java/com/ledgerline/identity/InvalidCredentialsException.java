package com.ledgerline.identity;

import com.ledgerline.DomainException;

/**
 * Wrong email or wrong password. The message never says which, so the sign-in form can't be used to
 * find out who has an account.
 */
class InvalidCredentialsException extends DomainException {

    InvalidCredentialsException() {
        super(Kind.UNAUTHENTICATED, "Sign-in failed", "Wrong email or password.", null);
    }
}
