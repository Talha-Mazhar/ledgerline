package com.ledgerline.assistant;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.StatementLine;
import com.ledgerline.wallets.WalletReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * The tools the model may call, bound to one customer. Each returns plain data; the model turns it
 * into an answer. Amounts are decimal strings, as everywhere in Ledgerline.
 */
class MoneyTools {

    /** One wallet's balance. */
    record Balance(String currency, String amount) {}

    /** One line of activity. */
    record Line(String date, String currency, String description, String amount) {}

    /** Money in and out per currency over a period, and where the money went out to. */
    record Summary(
            String from,
            String to,
            Map<String, String> moneyIn,
            Map<String, String> moneyOut,
            List<Spending> biggestOutgoing) {}

    /** Outgoing money grouped by description, e.g. everything sent to one person. */
    record Spending(String description, String currency, String total) {}

    private static final int MAX_LINES = 500;

    private final UUID customer;
    private final WalletReader wallets;

    MoneyTools(UUID customer, WalletReader wallets) {
        this.customer = customer;
        this.wallets = wallets;
    }

    @Tool(description = "The current balance of each of the customer's wallets.")
    List<Balance> balances() {
        return wallets.balancesOf(customer).stream()
                .map(money -> new Balance(money.currency().name(), money.toPlainString()))
                .toList();
    }

    @Tool(description = "The customer's activity between two dates (inclusive), oldest first: date, "
            + "currency, description and signed amount (negative is money out).")
    List<Line> activity(
            @ToolParam(description = "First day, as YYYY-MM-DD") String fromDate,
            @ToolParam(description = "Last day, as YYYY-MM-DD") String toDate) {
        return between(fromDate, toDate).stream()
                .map(line -> new Line(
                        line.postedAt().atZone(ZoneOffset.UTC).toLocalDate().toString(),
                        line.amount().currency().name(),
                        line.memo(),
                        line.amount().toPlainString()))
                .toList();
    }

    @Tool(description = "Totals between two dates (inclusive): money in and out per currency, and the "
            + "outgoing money grouped by description, largest first. Use this for questions like "
            + "'where did my money go'.")
    Summary summary(
            @ToolParam(description = "First day, as YYYY-MM-DD") String fromDate,
            @ToolParam(description = "Last day, as YYYY-MM-DD") String toDate) {
        Map<CurrencyCode, BigDecimal> in = new EnumMap<>(CurrencyCode.class);
        Map<CurrencyCode, BigDecimal> out = new EnumMap<>(CurrencyCode.class);
        Map<String, BigDecimal> byDescription = new LinkedHashMap<>();
        Map<String, CurrencyCode> currencyOf = new LinkedHashMap<>();
        for (StatementLine line : between(fromDate, toDate)) {
            BigDecimal amount = line.amount().amount();
            CurrencyCode currency = line.amount().currency();
            if (amount.signum() > 0) {
                in.merge(currency, amount, BigDecimal::add);
            } else {
                out.merge(currency, amount.negate(), BigDecimal::add);
                String key = line.memo() + " (" + currency + ")";
                byDescription.merge(key, amount.negate(), BigDecimal::add);
                currencyOf.put(key, currency);
            }
        }
        List<Spending> spending = new ArrayList<>();
        byDescription.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> spending.add(new Spending(
                        entry.getKey().substring(0, entry.getKey().lastIndexOf(" (")),
                        String.valueOf(currencyOf.get(entry.getKey())),
                        entry.getValue().toPlainString())));
        return new Summary(fromDate, toDate, strings(in), strings(out), spending);
    }

    private List<StatementLine> between(String fromDate, String toDate) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException wrongFormat) {
            throw new IllegalArgumentException("Dates must be YYYY-MM-DD", wrongFormat);
        }
        return wallets.activityBetween(
                customer,
                from.atStartOfDay().toInstant(ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                MAX_LINES);
    }

    private static Map<String, String> strings(Map<CurrencyCode, BigDecimal> totals) {
        Map<String, String> out = new LinkedHashMap<>();
        totals.forEach((currency, total) -> out.put(currency.name(), total.setScale(currency.minorUnits()).toPlainString()));
        return out;
    }
}
