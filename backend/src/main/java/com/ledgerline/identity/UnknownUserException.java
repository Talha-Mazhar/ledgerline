package com.ledgerline.identity;

import com.ledgerline.DomainException;

class UnknownUserException extends DomainException {

    UnknownUserException() {
        super(Kind.NOT_FOUND, "User not found", "There is no user with this id.", null);
    }
}
