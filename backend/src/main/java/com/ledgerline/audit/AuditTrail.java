package com.ledgerline.audit;

import com.ledgerline.identity.RoleChanged;
import com.ledgerline.onboarding.VerificationReviewed;
import com.ledgerline.onboarding.VerificationSubmitted;
import com.ledgerline.payments.PaymentApproved;
import com.ledgerline.payments.PaymentHeld;
import com.ledgerline.payments.PaymentRefunded;
import com.ledgerline.payments.PaymentRejected;
import java.time.Clock;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Writes an audit entry for every back-office action and every payment held for review. */
@Component
class AuditTrail {

    private final AuditLog log;
    private final Clock clock;

    AuditTrail(AuditLog log, Clock clock) {
        this.log = log;
        this.clock = clock;
    }

    @ApplicationModuleListener
    void on(PaymentHeld event) {
        log.record(
                null,
                "PAYMENT_HELD",
                "PAYMENT",
                event.paymentId(),
                event.kind() + " of " + event.amount() + " " + event.currency() + " held. " + event.reasons(),
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(PaymentApproved event) {
        log.record(
                event.approverId(),
                event.finalApproval() ? "PAYMENT_APPROVED_FINAL" : "PAYMENT_APPROVED_FIRST",
                "PAYMENT",
                event.paymentId(),
                event.finalApproval() ? "Second approval: money released." : "First approval: waiting for a second.",
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(PaymentRejected event) {
        log.record(
                event.rejectedBy(),
                "PAYMENT_REJECTED",
                "PAYMENT",
                event.paymentId(),
                "Rejected: " + event.reason(),
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(PaymentRefunded event) {
        log.record(
                event.refundedBy(),
                "PAYMENT_REFUNDED",
                "PAYMENT",
                event.paymentId(),
                "Refunded " + event.amount() + " " + event.currency() + ".",
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(VerificationSubmitted event) {
        log.record(event.userId(), "VERIFICATION_SUBMITTED", "USER", event.userId(), "Identity document uploaded.",
                clock.instant());
    }

    @ApplicationModuleListener
    void on(VerificationReviewed event) {
        String reason = event.reason();
        log.record(
                event.reviewerId(),
                event.approved() ? "VERIFICATION_APPROVED" : "VERIFICATION_REJECTED",
                "USER",
                event.userId(),
                event.approved() ? "Identity verified." : "Rejected: " + (reason == null ? "" : reason),
                clock.instant());
    }

    @ApplicationModuleListener
    void on(RoleChanged event) {
        log.record(
                event.actorId(),
                "ROLE_CHANGED",
                "USER",
                event.userId(),
                "Role changed from " + event.from() + " to " + event.to() + ".",
                clock.instant());
    }
}
