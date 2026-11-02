package com.ledgerline.onboarding;

/** Where a customer's verification stands. {@code NOT_STARTED} has no row in the table. */
public enum VerificationStatus {
    NOT_STARTED,
    PENDING_REVIEW,
    VERIFIED,
    REJECTED
}
