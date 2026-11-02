package com.ledgerline.fx;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.ledger.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Rates are read through the Redis cache, and storing new rates empties it. Not run in a
 * rolled-back transaction: the cache only takes a value once the transaction that read it commits.
 * Instead the seeded rates are put back afterwards, and the cache cleared, so other tests keep
 * seeing them.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RateStorageTests {

    private final JdbcExchangeRates rates;
    private final CacheManager caches;
    private final JdbcClient jdbc;
    private List<Map<String, @Nullable Object>> seeded = List.of();

    RateStorageTests(
            @Autowired JdbcExchangeRates rates, @Autowired CacheManager caches, @Autowired JdbcClient jdbc) {
        this.rates = rates;
        this.caches = caches;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void rememberTheSeededRates() {
        seeded = jdbc.sql("SELECT currency, units_per_eur, as_of FROM exchange_rates").query().listOfRows();
    }

    @AfterEach
    void restoreTheSeededRates() {
        for (Map<String, Object> row : seeded) {
            jdbc.sql("UPDATE exchange_rates SET units_per_eur = :rate, as_of = :asOf WHERE currency = :currency")
                    .param("rate", Objects.requireNonNull(row.get("units_per_eur")))
                    .param("asOf", Objects.requireNonNull(row.get("as_of")))
                    .param("currency", Objects.requireNonNull(row.get("currency")))
                    .update();
        }
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
