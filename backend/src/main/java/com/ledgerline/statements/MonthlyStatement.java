package com.ledgerline.statements;

import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.StatementLine;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * One wallet's month: what it opened with, every line, what it closed with.
 *
 * @param moneyIn the sum of all positive lines
 * @param moneyOut the sum of all negative lines (a negative number)
 */
record MonthlyStatement(
        UUID walletId,
        String ownerName,
        String ownerEmail,
        YearMonth month,
        Money opening,
        List<StatementLine> lines,
        Money moneyIn,
        Money moneyOut,
        Money closing) {

    MonthlyStatement {
        lines = List.copyOf(lines);
        BigDecimal expected = opening.amount().add(moneyIn.amount()).add(moneyOut.amount());
        if (expected.compareTo(closing.amount()) != 0) {
            throw new IllegalStateException("Statement for " + walletId + " " + month + " doesn't add up");
        }
    }
}
