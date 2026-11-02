package com.ledgerline.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The events this module reads, as this module sees them: only the fields it needs, as plain
 * strings and ids. They mirror the publishers' JSON, not their classes, so a publisher can add a
 * field without breaking anything here.
 */
final class Messages {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentCompleted(
            UUID eventId, UUID paymentId, String kind, UUID payerId, @Nullable UUID payeeId, String amount, String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentHeld(
            UUID eventId, UUID paymentId, String kind, UUID payerId, String amount, String currency, String reasons) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentRejected(
            UUID eventId, UUID paymentId, String kind, UUID payerId, String reason, String amount, String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentRefunded(
            UUID eventId,
            UUID paymentId,
            String kind,
            UUID payerId,
            @Nullable UUID payeeId,
            String amount,
            String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserRegistered(UUID eventId, UUID userId, String email, String displayName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VerificationSubmitted(UUID eventId, UUID userId, String displayName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VerificationReviewed(UUID eventId, UUID userId, boolean approved, @Nullable String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StatementReady(UUID eventId, UUID userId, UUID walletId, String currency, String month) {}

    private Messages() {}
}
