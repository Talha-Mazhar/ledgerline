package com.ledgerline.onboarding;

import java.util.UUID;

/** The onboarding module's public API for other modules: a customer's verification tier. */
public interface CustomerTiers {

    Tier tierOf(UUID userId);
}
