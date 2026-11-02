package com.ledgerline.ledger;

/**
 * Ledgerline's own accounts, one of each per currency. They are the other side of every movement
 * that crosses Ledgerline's boundary, and they may go negative: a negative funding balance is
 * simply the total that users have deposited.
 */
public enum SystemAccount {
    /** Where deposited money comes from. */
    FUNDING,
    /** Where withdrawn money goes. */
    PAYOUT,
    /** Buys one currency and sells another during an exchange. */
    FX,
    /** Holds money that has left a wallet but waits for approval before it reaches anyone. */
    HOLD
}
