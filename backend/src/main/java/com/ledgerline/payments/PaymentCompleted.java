package com.ledgerline.payments;

import com.ledgerline.ledger.CurrencyCode;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.modulith.events.Externalized;

/**
 * Money moved. Published to Kafka (topic {@code ledgerline.payments.completed}, keyed by payment
 * so all events of one payment stay in order), where the notifications module tells the people
 * involved.
 *
 * @param payerId who the money came from (for a deposit, the wallet owner)
 * @param payeeId who received it, for transfers and refunds
 */
@Externalized("ledgerline.payments.completed::#{#this.paymentId().toString()}")
public record PaymentCompleted(
        UUID eventId,
        UUID paymentId,
        PaymentKind kind,
        UUID payerId,
        @Nullable UUID payeeId,
        String amount,
        CurrencyCode currency,
        Instant occurredAt) {}
