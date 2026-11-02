package com.ledgerline.onboarding;

/** How far a customer is verified. Limits depend on it. */
public enum Tier {
    /** Signed up, identity not verified yet: low limits. */
    BASIC,
    /** Identity document approved by staff: higher limits. */
    VERIFIED
}
