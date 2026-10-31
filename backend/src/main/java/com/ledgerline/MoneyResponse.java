package com.ledgerline;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;

/**
 * Money as JSON: {@code {"amount": "1250.00", "currency": "EUR"}}, the contract's {@code Money}
 * schema. The amount is a decimal string, so no client ever parses money as a floating-point number.
 * Shared by every module that returns money.
 */
public record MoneyResponse(String amount, CurrencyCode currency) {

    public static MoneyResponse from(Money money) {
        return new MoneyResponse(money.toPlainString(), money.currency());
    }
}
