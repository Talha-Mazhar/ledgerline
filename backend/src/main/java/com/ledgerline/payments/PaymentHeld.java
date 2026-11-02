package com.ledgerline.payments;

import com.ledgerline.ledger.CurrencyCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/** A payment waits for two staff approvals. The customer and the staff are notified. */
@Externalized("ledgerline.payments.held")
public record PaymentHeld(
        UUID eventId,
        UUID paymentId,
        PaymentKind kind,
        UUID payerId,
        String amount,
        CurrencyCode currency,
        String reasons,
        Instant occurredAt) {}
