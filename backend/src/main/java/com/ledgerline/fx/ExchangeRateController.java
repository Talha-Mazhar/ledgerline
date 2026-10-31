package com.ledgerline.fx;

import com.ledgerline.ledger.CurrencyCode;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/exchange-rates?from=EUR&to=CZK}. */
@RestController
class ExchangeRateController {

    record ExchangeRateResponse(CurrencyCode from, CurrencyCode to, String rate, String midRate, Instant asOf) {}

    private final ExchangeRates rates;

    ExchangeRateController(ExchangeRates rates) {
        this.rates = rates;
    }

    @GetMapping("/api/v1/exchange-rates")
    public ExchangeRateResponse getExchangeRate(@RequestParam CurrencyCode from, @RequestParam CurrencyCode to) {
        Rate rate = rates.rate(from, to);
        return new ExchangeRateResponse(
                rate.from(),
                rate.to(),
                rate.customerRate().toPlainString(),
                rate.midRate().toPlainString(),
                rate.asOf());
    }
}
