package com.ledgerline.notifications;

import java.math.BigDecimal;
import java.util.Locale;

/** Amounts written for people: {@code €1,250.00}, {@code Kč 2,502.42}, like the app shows them. */
final class Amounts {

    static String format(String amount, String currency) {
        String symbol = switch (currency) {
            case "EUR" -> "€";
            case "USD" -> "$";
            case "GBP" -> "£";
            case "CZK" -> "Kč ";
            default -> currency + " ";
        };
        return symbol + String.format(Locale.ROOT, "%,.2f", new BigDecimal(amount));
    }

    private Amounts() {}
}
