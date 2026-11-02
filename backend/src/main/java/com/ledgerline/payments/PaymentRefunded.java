package com.ledgerline.payments;

import com.ledgerline.ledger.CurrencyCode;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.modulith.events.Externalized;

/** Staff reversed a completed payment. */
@Externalized("ledgerline.payments.refunded::#{#this.paymentId().toString()}")
public record PaymentRefunded(
        UUID eventId,
        UUID paymentId,
        UUID refundId,
        PaymentKind kind,
        UUID refundedBy,
        UUID payerId,
        @Nullable UUID payeeId,
        String amount,
        CurrencyCode currency,
        Instant occurredAt) {}
