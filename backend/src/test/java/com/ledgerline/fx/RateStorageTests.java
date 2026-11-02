package com.ledgerline.fx;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.ledger.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rates are read through the Redis cache, and storing new rates empties it. Rolled back
 * afterwards, and the cache cleared, so other tests keep seeing the seeded rates.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RateStorageTests {

    private final JdbcExchangeRates rates;
    private final CacheManager caches;

    RateStorageTests(@Autowired JdbcExchangeRates rates, @Autowired CacheManager caches) {
        this.rates = rates;
        this.caches = caches;
    }

    @AfterEach
    void clearTheCache() {
        Objects.requireNonNull(caches.getCache(JdbcExchangeRates.CACHE)).clear();
    }

    @Test
    void newRatesReplaceCachedOnes() {
        assertThat(rates.rate(CurrencyCode.EUR, CurrencyCode.CZK).midRate()).isEqualByComparingTo("25.15");
        assertThat(Objects.requireNonNull(caches.getCache(JdbcExchangeRates.CACHE)).get("EUR-CZK"))
                .as("the rate is cached")
                .isNotNull();

        boolean changed = rates.store(
                Map.of(CurrencyCode.CZK, new BigDecimal("24.31"), CurrencyCode.USD, new BigDecimal("1.0874")),
                Instant.now());

        assertThat(changed).isTrue();
        assertThat(rates.rate(CurrencyCode.EUR, CurrencyCode.CZK).midRate()).isEqualByComparingTo("24.31");
    }

    @Test
    void olderRatesDontOverwriteNewerOnes() {
        assertThat(rates.store(Map.of(CurrencyCode.CZK, new BigDecimal("30")), Instant.parse("2020-01-01T00:00:00Z")))
                .isFalse();
        assertThat(rates.rate(CurrencyCode.EUR, CurrencyCode.CZK).midRate()).isEqualByComparingTo("25.15");
    }
}
