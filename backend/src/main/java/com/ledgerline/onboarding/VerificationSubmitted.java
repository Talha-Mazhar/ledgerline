package com.ledgerline.onboarding;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/** A customer uploaded an identity document. Staff are notified that a review is waiting. */
@Externalized("ledgerline.onboarding.verification-submitted")
public record VerificationSubmitted(UUID eventId, UUID userId, String displayName) {}
