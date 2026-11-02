package com.ledgerline.statements;

import com.ledgerline.identity.UserDirectory;
import com.ledgerline.identity.UserSummary;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.StatementLine;
import com.ledgerline.wallets.OwnedWallet;
import com.ledgerline.wallets.WalletDirectory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds statements from the ledger. Months are calendar months in UTC. */
@Service
class Statements {

    private final WalletDirectory wallets;
    private final Ledger ledger;
    private final UserDirectory users;
    private final Clock clock;

    Statements(WalletDirectory wallets, Ledger ledger, UserDirectory users, Clock clock) {
        this.wallets = wallets;
        this.ledger = ledger;
        this.users = users;
        this.clock = clock;
    }

    /** The months a wallet has statements for: from the month it was opened to this one, newest first. */
    @Transactional(readOnly = true)
    List<YearMonth> monthsOf(UUID ownerId, UUID walletId) {
        OwnedWallet wallet = wallets.requireOwned(ownerId, walletId);
        YearMonth first = YearMonth.from(wallet.openedAt().atZone(ZoneOffset.UTC));
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth month = YearMonth.now(clock.withZone(ZoneOffset.UTC));
                !month.isBefore(first);
                month = month.minusMonths(1)) {
            months.add(month);
        }
        return months;
    }

    @Transactional(readOnly = true)
    MonthlyStatement statement(UUID ownerId, UUID walletId, YearMonth month) {
        return build(wallets.requireOwned(ownerId, walletId), month);
    }

    @Transactional(readOnly = true)
    MonthlyStatement build(OwnedWallet wallet, YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Money opening = ledger.balanceBefore(wallet.id(), from);
        List<StatementLine> lines = ledger.linesBetween(wallet.id(), from, to);
        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        for (StatementLine line : lines) {
            if (line.amount().isPositive()) {
                in = in.add(line.amount().amount());
            } else {
                out = out.add(line.amount().amount());
            }
        }
        Money closing = lines.isEmpty() ? opening : lines.getLast().balanceAfter();
        UserSummary owner = users.require(wallet.ownerId());
        return new MonthlyStatement(
                wallet.id(),
                owner.displayName(),
                owner.email(),
                month,
                opening,
                lines,
                new Money(in, wallet.currency()),
                new Money(out, wallet.currency()),
                closing);
    }
}
