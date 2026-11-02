package com.ledgerline;

import org.jspecify.annotations.Nullable;

/**
 * A business rule said no. Every module's errors extend this class, and {@link ProblemDetailsAdvice}
 * turns them into RFC 9457 problem details, so all modules answer errors the same way.
 *
 * <p>The {@link Kind} says what went wrong in business terms; only the advice knows which HTTP
 * status that becomes, so services stay free of HTTP. The message is written for the end user: it
 * becomes the problem's {@code detail}, which the frontend shows as is.
 */
public abstract class DomainException extends RuntimeException {

    /** What kind of refusal this is. Each kind maps to one HTTP status. */
    public enum Kind {
        /** The request itself is invalid, e.g. an amount of zero. 400. */
        INVALID_REQUEST,
        /** The user couldn't be identified, e.g. a wrong password. 401. */
        UNAUTHENTICATED,
        /** The thing asked for doesn't exist for this user. 404. */
        NOT_FOUND,
        /** The request clashes with the current state, e.g. a wallet that already exists. 409. */
        CONFLICT,
        /** Well-formed, but a business rule forbids it, e.g. not enough money. 422. */
        RULE_VIOLATION,
        /** A service the request needs is down, e.g. the assistant's model. 503. */
        UNAVAILABLE
    }

    private final Kind kind;
    private final String title;

    protected DomainException(Kind kind, String title, String message, @Nullable Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.title = title;
    }

    public Kind kind() {
        return kind;
    }

    /** A short summary that is the same for every error of this type, e.g. "Not enough money". */
    public String title() {
        return title;
    }
}
