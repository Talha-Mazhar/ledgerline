package com.ledgerline.statements;

import com.ledgerline.DomainException;

class InvalidMonthException extends DomainException {

    InvalidMonthException() {
        super(Kind.INVALID_REQUEST, "Invalid month", "Give the month as YYYY-MM, e.g. 2026-10.", null);
    }
}
