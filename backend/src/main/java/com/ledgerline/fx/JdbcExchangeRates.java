package com.ledgerline.fx;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rates from the {@code exchange_rates} table. Each currency is stored against the euro, so the
 * rate from A to B is {@code B per euro / A per euro}: 1 USD in CZK is 25.15 / 1.085.
 *
 * <p>Ledgerline's spread is 0.5%: the customer rate is the mid rate times 0.995.
 */
@Service
class JdbcExchangeRates implements ExchangeRates {

    static final BigDecimal SPREAD = new BigDecimal("0.005");

    private final JdbcClient jdbc;

    JdbcExchangeRates(JdbcClient jdbc) {
        this.jdbc = jdbc;
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

    @Override
    @Transactional(readOnly = true)
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
