package com.ledgerline.messaging;

import java.util.List;

/** The Kafka topics events are published to, named after the {@code @Externalized} targets. */
public final class Topics {

    public static final String PAYMENT_COMPLETED = "ledgerline.payments.completed";
    public static final String PAYMENT_HELD = "ledgerline.payments.held";
    public static final String PAYMENT_REJECTED = "ledgerline.payments.rejected";
    public static final String PAYMENT_REFUNDED = "ledgerline.payments.refunded";
    public static final String USER_REGISTERED = "ledgerline.identity.user-registered";
    public static final String VERIFICATION_SUBMITTED = "ledgerline.onboarding.verification-submitted";
    public static final String VERIFICATION_REVIEWED = "ledgerline.onboarding.verification-reviewed";
    public static final String STATEMENT_READY = "ledgerline.statements.ready";

    public static final List<String> ALL = List.of(
            PAYMENT_COMPLETED,
            PAYMENT_HELD,
            PAYMENT_REJECTED,
            PAYMENT_REFUNDED,
            USER_REGISTERED,
            VERIFICATION_SUBMITTED,
            VERIFICATION_REVIEWED,
            STATEMENT_READY);

    private Topics() {}
}
