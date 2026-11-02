package com.ledgerline.fx;

import com.ledgerline.ledger.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Where fresh rates come from. One implementation calls the ECB's rates; tests use a fake. */
interface RateFeed {

    /** The latest rates: units of each currency per euro, and when they were set. */
    Rates latest();

    record Rates(Map<CurrencyCode, BigDecimal> unitsPerEur, Instant asOf) {
        Rates {
            unitsPerEur = Map.copyOf(unitsPerEur);
        }
    }
}
