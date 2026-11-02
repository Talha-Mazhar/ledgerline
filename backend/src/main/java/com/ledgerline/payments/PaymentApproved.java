package com.ledgerline.payments;

import java.time.Instant;
import java.util.UUID;

/**
 * A staff member approved a held payment. Stays inside the application (the audit log records
 * it); the customer hears about the outcome through {@link PaymentCompleted}.
 *
 * @param finalApproval true for the second approval, which released the money
 */
public record PaymentApproved(
        UUID eventId, UUID paymentId, UUID approverId, boolean finalApproval, Instant occurredAt) {}
