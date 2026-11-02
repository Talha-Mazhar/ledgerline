package com.ledgerline.payments;

import com.ledgerline.ledger.CurrencyCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/** Staff rejected a held payment; its money went back to the wallet. */
@Externalized("ledgerline.payments.rejected::#{#this.paymentId().toString()}")
public record PaymentRejected(
        UUID eventId,
        UUID paymentId,
        PaymentKind kind,
        UUID payerId,
        UUID rejectedBy,
        String reason,
        String amount,
        CurrencyCode currency,
        Instant occurredAt) {}
