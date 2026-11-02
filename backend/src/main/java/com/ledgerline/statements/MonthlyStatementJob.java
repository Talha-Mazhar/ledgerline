package com.ledgerline.statements;

import com.ledgerline.documents.DocumentStore;
import com.ledgerline.wallets.OwnedWallet;
import com.ledgerline.wallets.WalletDirectory;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * On the first of each month at 03:00 UTC, archives last month's statement of every wallet that
 * had any activity, and tells its owner. Runs on one instance only (ShedLock).
 */
@Component
class MonthlyStatementJob {

    private static final Logger log = LoggerFactory.getLogger(MonthlyStatementJob.class);

    private final WalletDirectory wallets;
    private final Statements statements;
    private final StatementPdf pdf;
    private final DocumentStore documents;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    MonthlyStatementJob(
            WalletDirectory wallets,
            Statements statements,
            StatementPdf pdf,
            DocumentStore documents,
            ApplicationEventPublisher events,
            Clock clock) {
        this.wallets = wallets;
        this.statements = statements;
        this.pdf = pdf;
        this.documents = documents;
        this.events = events;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 1 * *", zone = "UTC")
    @SchedulerLock(name = "monthly-statements", lockAtMostFor = "PT2H")
    void scheduledRun() {
        int archived = archive(YearMonth.now(clock.withZone(ZoneOffset.UTC)).minusMonths(1));
        log.info("Archived {} monthly statements", archived);
    }

    /** Archives last month's statements and returns how many there were. */
    @Transactional
    int archive(YearMonth month) {
        int archived = 0;
        for (OwnedWallet wallet : wallets.all()) {
            MonthlyStatement statement = statements.build(wallet, month);
            if (statement.lines().isEmpty()) {
                continue;
            }
            documents.put(key(wallet, month), pdf.render(statement), "application/pdf");
            events.publishEvent(new StatementReady(
                    UUID.randomUUID(), wallet.ownerId(), wallet.id(), wallet.currency().name(), month.toString()));
            archived++;
        }
        return archived;
    }

    static String key(OwnedWallet wallet, YearMonth month) {
        return "statements/" + wallet.ownerId() + "/" + wallet.id() + "/" + month + ".pdf";
    }
}
