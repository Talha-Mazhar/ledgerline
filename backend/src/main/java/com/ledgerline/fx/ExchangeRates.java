package com.ledgerline.fx;

import com.ledgerline.ledger.CurrencyCode;

/** The fx module's public API: the rate between two currencies. */
public interface ExchangeRates {

    /**
     * The rate for selling {@code from} and buying {@code to}.
     *
     * @throws com.ledgerline.DomainException (invalid request) if the two currencies are the same
     */
    Rate rate(CurrencyCode from, CurrencyCode to);
}
