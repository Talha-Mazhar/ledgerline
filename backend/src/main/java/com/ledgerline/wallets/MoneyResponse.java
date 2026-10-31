package com.ledgerline.wallets;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;

/**
 * Money as JSON: {@code {"amount": "1250.00", "currency": "EUR"}}, the contract's {@code Money}
 * schema. The amount is a decimal string, so no client ever parses money as a floating-point number.
 * Part of the wallets module's public API, so every module that returns money uses the same shape.
 * It lives here, not in the root package: root types are shared by every module, so a root type
 * that depended on the ledger would close a dependency cycle (ledger → root → ledger).
 */
public record MoneyResponse(String amount, CurrencyCode currency) {

    public static MoneyResponse from(Money money) {
        return new MoneyResponse(money.toPlainString(), money.currency());
    }
}
