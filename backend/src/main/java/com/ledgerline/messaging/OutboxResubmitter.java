package com.ledgerline.messaging;

import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends events again that couldn't be delivered, e.g. because Kafka was down. Runs every minute on
 * one instance only (ShedLock), for publications older than a minute, so it never races the
 * normal, immediate send.
 */
@Component
class OutboxResubmitter {

    static final Duration GRACE = Duration.ofMinutes(1);

    private final IncompleteEventPublications incomplete;

    OutboxResubmitter(IncompleteEventPublications incomplete) {
        this.incomplete = incomplete;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    @SchedulerLock(name = "outbox-resubmit", lockAtMostFor = "PT5M")
    void resubmit() {
        incomplete.resubmitIncompletePublicationsOlderThan(GRACE);
    }
}
