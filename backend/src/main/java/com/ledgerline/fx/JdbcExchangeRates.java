package com.ledgerline.fx;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Rates from the {@code exchange_rates} table. Each currency is stored against the euro, so the
 * rate from A to B is {@code B per euro / A per euro}: 1 USD in CZK is 25.15 / 1.085.
 *
 * <p>Ledgerline's spread is 0.5%: the customer rate is the mid rate times 0.995.
 */
@Service
class JdbcExchangeRates implements ExchangeRates {

    static final BigDecimal SPREAD = new BigDecimal("0.005");

    /** Rates are cached (Redis) for an hour, and evicted whenever new ones are stored. */
    static final String CACHE = "fx-rates";

    private final JdbcClient jdbc;
    private final CacheManager caches;

    JdbcExchangeRates(JdbcClient jdbc, CacheManager caches) {
        this.jdbc = jdbc;
        this.caches = caches;
    }

    @Override
    @Transactional(readOnly = true)
    public Money valueIn(Money amount, CurrencyCode currency) {
        if (amount.currency() == currency) {
            return amount;
        }
        BigDecimal mid = rate(amount.currency(), currency).midRate();
        return new Money(amount.amount().multiply(mid).setScale(currency.minorUnits(), RoundingMode.HALF_EVEN), currency);
    }

    /**
     * Stores fresh rates and empties the cache, so the next read sees them.
     *
     * @return true when at least one rate is newer than the stored one
     */
    @Transactional
    public boolean store(Map<CurrencyCode, BigDecimal> unitsPerEur, Instant asOf) {
        int changed = 0;
        for (Map.Entry<CurrencyCode, BigDecimal> rate : unitsPerEur.entrySet()) {
            changed += jdbc.sql("""
                            UPDATE exchange_rates SET units_per_eur = :rate, as_of = :asOf
                            WHERE currency = :currency AND as_of < :asOf
                            """)
                    .param("rate", rate.getValue())
                    .param("asOf", OffsetDateTime.ofInstant(asOf, ZoneOffset.UTC))
                    .param("currency", rate.getKey().name())
                    .update();
        }
        if (changed > 0) {
            afterCommit(this::emptyTheCache);
        }
        return changed > 0;
    }

    /**
     * Empties the cache once the new rates are committed, not before: a quote read in between
     * would otherwise put the old rate back in the cache.
     */
    private void emptyTheCache() {
        Cache cache = caches.getCache(CACHE);
        if (cache != null) {
            cache.invalidate();
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE, key = "#from.name() + '-' + #to.name()")
    public Rate rate(CurrencyCode from, CurrencyCode to) {
        if (from == to) {
            throw new SameCurrencyException();
        }
        record Row(CurrencyCode currency, BigDecimal unitsPerEur, Instant asOf) {}
        List<Row> rows = jdbc.sql("""
                        SELECT currency, units_per_eur, as_of FROM exchange_rates
                        WHERE currency IN (:from, :to)
                        """)
                .param("from", from.name())
                .param("to", to.name())
                .query((rs, row) -> new Row(
                        CurrencyCode.valueOf(rs.getString("currency")),
                        rs.getBigDecimal("units_per_eur"),
                        rs.getObject("as_of", OffsetDateTime.class).toInstant()))
                .list();
        Map<CurrencyCode, Row> byCurrency = new EnumMap<>(CurrencyCode.class);
        rows.forEach(row -> byCurrency.put(row.currency(), row));
        Row fromRow = byCurrency.get(from);
        Row toRow = byCurrency.get(to);
        if (fromRow == null || toRow == null) {
            throw new IllegalStateException("No exchange rate stored for " + from + " or " + to);
        }

        BigDecimal mid = toRow.unitsPerEur().divide(fromRow.unitsPerEur(), MathContext.DECIMAL64);
        BigDecimal customer = mid.multiply(BigDecimal.ONE.subtract(SPREAD));
        Instant asOf = fromRow.asOf().isAfter(toRow.asOf()) ? fromRow.asOf() : toRow.asOf();
        return new Rate(
                from,
                to,
                mid.setScale(Rate.SCALE, RoundingMode.HALF_EVEN),
                customer.setScale(Rate.SCALE, RoundingMode.HALF_EVEN),
                asOf);
    }
}
