package com.ledgerline.identity;

import com.ledgerline.DomainException;
import org.jspecify.annotations.Nullable;

class EmailTakenException extends DomainException {

    EmailTakenException(@Nullable Throwable cause) {
        super(
                Kind.CONFLICT,
                "Email already registered",
                "An account with this email already exists. Sign in instead.",
                cause);
    }
}
