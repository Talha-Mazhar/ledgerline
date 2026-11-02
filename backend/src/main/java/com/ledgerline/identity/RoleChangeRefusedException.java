package com.ledgerline.identity;

import com.ledgerline.DomainException;

class RoleChangeRefusedException extends DomainException {

    RoleChangeRefusedException(String message) {
        super(Kind.RULE_VIOLATION, "Role can't be changed", message, null);
    }
}
