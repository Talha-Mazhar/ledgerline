package com.ledgerline.payments;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import java.math.BigDecimal;

/**
 * Turns the amount a user typed ({@code "250"}, {@code "250.5"}) into {@link Money}. The contract's
 * pattern already limits the format; this also refuses zero and checks the currency's decimals.
 */
final class Amounts {

    private Amounts() {}

    static Money positive(String typed, CurrencyCode currency) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(typed.trim());
        } catch (NumberFormatException notANumber) {
            throw new InvalidAmountException("Enter an amount like 250 or 250.50.");
        }
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Enter an amount greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > currency.minorUnits()) {
            throw new InvalidAmountException(
                    currency + " amounts have at most " + currency.minorUnits() + " decimal places.");
        }
        return new Money(amount, currency);
    }
}
