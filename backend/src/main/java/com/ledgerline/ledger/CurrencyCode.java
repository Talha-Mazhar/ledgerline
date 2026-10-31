package com.ledgerline.ledger;

/**
 * The currencies Ledgerline supports, by ISO 4217 code.
 *
 * <p>Part of the ledger's public API: every module that handles money uses these codes, so a new
 * currency is added here once.
 */
public enum CurrencyCode {
    EUR(2),
    USD(2),
    CZK(2),
    GBP(2);

    private final int minorUnits;

    CurrencyCode(int minorUnits) {
        this.minorUnits = minorUnits;
    }

    /** Decimal places of the currency's smallest unit: 2 for cents, pence and haléře. */
    public int minorUnits() {
        return minorUnits;
    }
}
