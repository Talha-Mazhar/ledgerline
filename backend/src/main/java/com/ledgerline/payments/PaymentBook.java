package com.ledgerline.payments;

import com.ledgerline.ledger.IdempotencyKeyReusedException;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.StatementLine;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * What every kind of payment does the same way: find an earlier attempt with the same
 * Idempotency-Key, save the payment, publish its events.
 *
 * <p>How a retry is recognised: the payment row has a unique (user, key) pair. A retry that finds
 * it gets the first result back. Two attempts racing each other both reach the ledger, whose own
 * unique key makes the second wait for the first and then replay it; the second then finds the
 * first's payment row, committed in the same transaction as its ledger entry.
 */
@Component
class PaymentBook {

    private final PaymentRepository payments;
    private final Ledger ledger;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final MeterRegistry metrics;

    PaymentBook(
            PaymentRepository payments,
            Ledger ledger,
            ApplicationEventPublisher events,
            Clock clock,
            MeterRegistry metrics) {
        this.payments = payments;
        this.metrics = metrics;
        this.ledger = ledger;
        this.events = events;
        this.clock = clock;
    }

    /**
     * The payment an earlier request with this key made, if any.
     *
     * @throws IdempotencyKeyReusedException if that request was a different one
     */
    Optional<Payment> earlier(UUID user, UUID key, String fingerprint) {
        return payments.findByInitiatedByAndIdempotencyKey(user, key.toString()).map(payment -> {
            if (!payment.fingerprint().equals(fingerprint)) {
                throw new IdempotencyKeyReusedException();
            }
            return payment;
        });
    }

    /** The payment a racing request saved, after the ledger replayed its entry. */
    Payment afterReplay(UUID user, UUID key, String fingerprint) {
        return earlier(user, key, fingerprint)
                .orElseThrow(() -> new IllegalStateException("The ledger replayed an entry that has no payment"));
    }

    /** The line the payment's first entry wrote on a wallet. */
    StatementLine lineOf(Payment payment, UUID walletId) {
        return ledger.entry(payment.entryId()).lineFor(walletId);
    }

    Payment save(Payment payment) {
        Payment saved = payments.save(payment);
        count(saved);
        return saved;
    }

    /** Counts payments by kind and status (Prometheus: {@code ledgerline_payments_total}). */
    void count(Payment payment) {
        metrics.counter(
                        "ledgerline.payments",
                        "kind",
                        payment.kind().name(),
                        "status",
                        payment.status().name())
                .increment();
    }

    void publish(Object event) {
        events.publishEvent(event);
    }

    Instant now() {
        return clock.instant();
    }

    PaymentCompleted completed(Payment payment) {
        return new PaymentCompleted(
                UUID.randomUUID(),
                payment.id(),
                payment.kind(),
                payment.initiatedBy(),
                payment.counterpartyId(),
                payment.amount().toPlainString(),
                payment.amount().currency(),
                payment.updatedAt());
    }

    PaymentHeld held(Payment payment) {
        String reasons = payment.holdReason();
        return new PaymentHeld(
                UUID.randomUUID(),
                payment.id(),
                payment.kind(),
                payment.initiatedBy(),
                payment.amount().toPlainString(),
                payment.amount().currency(),
                reasons == null ? "" : reasons,
                payment.createdAt());
    }
}
