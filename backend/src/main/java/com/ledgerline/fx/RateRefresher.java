package com.ledgerline.fx;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Fetches the latest rates every hour (and soon after start-up), on one instance only. */
@Component
class RateRefresher {

    private static final Logger log = LoggerFactory.getLogger(RateRefresher.class);

    private final RateFeed feed;
    private final JdbcExchangeRates rates;

    RateRefresher(RateFeed feed, JdbcExchangeRates rates) {
        this.feed = feed;
        this.rates = rates;
    }

    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "PT1H")
    @SchedulerLock(name = "fx-refresh", lockAtMostFor = "PT10M")
    void scheduledRefresh() {
        try {
            refresh();
        } catch (RuntimeException failed) {
            log.warn("Exchange rates not refreshed, keeping the last ones: {}", failed.getMessage());
        }
    }

    /** @return true when stored rates changed */
    boolean refresh() {
        RateFeed.Rates latest = feed.latest();
        return rates.store(latest.unitsPerEur(), latest.asOf());
    }
}
