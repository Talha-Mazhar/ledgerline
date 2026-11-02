package com.ledgerline.onboarding;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.modulith.events.Externalized;

/** Staff approved or rejected a customer's verification. The customer is notified. */
@Externalized("ledgerline.onboarding.verification-reviewed")
public record VerificationReviewed(
        UUID eventId,
        UUID userId,
        UUID reviewerId,
        boolean approved,
        @Nullable String reason) {}
