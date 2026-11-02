package com.ledgerline.fx;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;

/** The fx module's public API: the rate between two currencies. */
public interface ExchangeRates {

    /**
     * The rate for selling {@code from} and buying {@code to}.
     *
     * @throws com.ledgerline.DomainException (invalid request) if the two currencies are the same
     */
    Rate rate(CurrencyCode from, CurrencyCode to);

    /**
     * The amount's value in another currency at the mid rate, without Ledgerline's spread. For
     * comparing amounts in different currencies (limits, reports), never for paying anyone.
     */
    Money valueIn(Money amount, CurrencyCode currency);
}
