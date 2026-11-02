package com.ledgerline.fx;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * The rate for selling {@code from} and buying {@code to}.
 *
 * @param midRate the market rate, halfway between buying and selling
 * @param customerRate what Ledgerline offers: the mid rate less its spread. This is the rate used
 * @param asOf when the underlying rates were set
 */
public record Rate(CurrencyCode from, CurrencyCode to, BigDecimal midRate, BigDecimal customerRate, Instant asOf)
        implements Serializable {

    /** Rates are shown and applied with six decimal places. */
    public static final int SCALE = 6;

    /**
     * How much of {@code to} the given amount of {@code from} buys at the customer rate.
     *
     * <p>Rounded down to the currency's smallest unit: the user never receives a fraction of a cent
     * more than the rate pays for, and the difference stays in Ledgerline's FX account.
     */
    public Money convert(Money sold) {
        if (sold.currency() != from) {
            throw new IllegalArgumentException("This rate sells " + from + ", not " + sold.currency());
        }
        BigDecimal bought = sold.amount().multiply(customerRate).setScale(to.minorUnits(), RoundingMode.DOWN);
        return new Money(bought, to);
    }
}
