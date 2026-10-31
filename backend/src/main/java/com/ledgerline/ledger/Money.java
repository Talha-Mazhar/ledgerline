package com.ledgerline.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount in one currency.
 *
 * <p>Amounts are {@link BigDecimal}, never {@code double}: binary floating point cannot represent
 * 0.10 exactly, and those errors add up in a ledger. The amount always has exactly the currency's
 * number of decimal places, so {@code 12.5 EUR} is stored and printed as {@code 12.50}. An amount
 * with more decimals than the currency allows is rejected rather than rounded, because silently
 * rounding money hides bugs.
 */
public record Money(BigDecimal amount, CurrencyCode currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.minorUnits(), RoundingMode.UNNECESSARY);
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /** The same amount with the opposite sign: money in becomes money out. */
    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    /** The amount as a plain decimal string, e.g. {@code "1250.00"}: the format the API uses. */
    public String toPlainString() {
        return amount.toPlainString();
    }
}
