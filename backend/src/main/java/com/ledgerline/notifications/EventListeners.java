package com.ledgerline.notifications;

import com.ledgerline.messaging.Topics;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads events from Kafka and turns them into notifications. A listener that throws is retried
 * (see messaging's retry topics), so these methods may run more than once for the same message;
 * the inbox makes that harmless.
 */
@Component
class EventListeners {

    private static final String PAYMENTS = "/payments";

    private final JsonMapper json;
    private final Notifier notifier;

    EventListeners(JsonMapper json, Notifier notifier) {
        this.json = json;
        this.notifier = notifier;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    void paymentCompleted(String message) {
        Messages.PaymentCompleted event = json.readValue(message, Messages.PaymentCompleted.class);
        String amount = Amounts.format(event.amount(), event.currency());
        switch (event.kind()) {
            case "DEPOSIT" -> notifier.tell(event.payerId(), event.eventId(), "MONEY_ADDED", "Money added",
                    amount + " was added to your " + event.currency() + " wallet.", PAYMENTS, true);
            case "WITHDRAWAL" -> notifier.tell(event.payerId(), event.eventId(), "WITHDRAWAL_SENT",
                    "Withdrawal on its way", amount + " is on its way to your bank.", PAYMENTS, true);
            case "EXCHANGE" -> notifier.tell(event.payerId(), event.eventId(), "EXCHANGED", "Exchange done",
                    "You exchanged " + amount + ".", PAYMENTS, false);
            case "TRANSFER" -> {
                notifier.tell(event.payerId(), event.eventId(), "MONEY_SENT", "Money sent",
                        "You sent " + amount + " to " + notifier.nameOf(event.payeeId()) + ".", PAYMENTS, true);
                UUID payee = event.payeeId();
                if (payee != null) {
                    notifier.tell(payee, event.eventId(), "MONEY_RECEIVED", "Money received",
                            notifier.nameOf(event.payerId()) + " sent you " + amount + ".", PAYMENTS, true);
                }
            }
            default -> {
                // Other kinds (refunds) are announced by their own event.
            }
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_HELD)
    void paymentHeld(String message) {
        Messages.PaymentHeld event = json.readValue(message, Messages.PaymentHeld.class);
        String what = describe(event.kind()) + " of " + Amounts.format(event.amount(), event.currency());
        notifier.tell(event.payerId(), event.eventId(), "PAYMENT_HELD", "Payment waiting for approval",
                "Your " + what + " needs a quick check by our team before it goes through. " + event.reasons(),
                PAYMENTS, true);
        notifier.tellStaff(event.eventId(), "APPROVAL_NEEDED", "Payment needs approval",
                notifier.nameOf(event.payerId()) + "'s " + what + " is waiting for two approvals.",
                "/back-office/approvals");
    }

    @KafkaListener(topics = Topics.PAYMENT_REJECTED)
    void paymentRejected(String message) {
        Messages.PaymentRejected event = json.readValue(message, Messages.PaymentRejected.class);
        String what = describe(event.kind()) + " of " + Amounts.format(event.amount(), event.currency());
        notifier.tell(event.payerId(), event.eventId(), "PAYMENT_REJECTED", "Payment not approved",
                "Your " + what + " wasn't approved, and the money is back in your wallet. Reason: " + event.reason(),
                PAYMENTS, true);
    }

    @KafkaListener(topics = Topics.PAYMENT_REFUNDED)
    void paymentRefunded(String message) {
        Messages.PaymentRefunded event = json.readValue(message, Messages.PaymentRefunded.class);
        String amount = Amounts.format(event.amount(), event.currency());
        if (event.kind().equals("DEPOSIT")) {
            notifier.tell(event.payerId(), event.eventId(), "REFUNDED", "Top-up refunded",
                    amount + " was refunded to your card.", PAYMENTS, true);
            return;
        }
        notifier.tell(event.payerId(), event.eventId(), "REFUNDED", "Refund received",
                amount + " from your transfer to " + notifier.nameOf(event.payeeId()) + " was refunded to you.",
                PAYMENTS, true);
        UUID payee = event.payeeId();
        if (payee != null) {
            notifier.tell(payee, event.eventId(), "REFUNDED", "Transfer refunded",
                    "The " + amount + " you received from " + notifier.nameOf(event.payerId())
                            + " was refunded to them.",
                    PAYMENTS, true);
        }
    }

    @KafkaListener(topics = Topics.USER_REGISTERED)
    void userRegistered(String message) {
        Messages.UserRegistered event = json.readValue(message, Messages.UserRegistered.class);
        notifier.tell(event.userId(), event.eventId(), "WELCOME", "Welcome to Ledgerline",
                "Hi " + event.displayName() + ", open a wallet to get started. Verify your identity to raise your limits.",
                "/verification", true);
    }

    @KafkaListener(topics = Topics.VERIFICATION_SUBMITTED)
    void verificationSubmitted(String message) {
        Messages.VerificationSubmitted event = json.readValue(message, Messages.VerificationSubmitted.class);
        notifier.tellStaff(event.eventId(), "VERIFICATION_TO_REVIEW", "Verification to review",
                event.displayName() + " uploaded an identity document.", "/back-office/verifications");
    }

    @KafkaListener(topics = Topics.VERIFICATION_REVIEWED)
    void verificationReviewed(String message) {
        Messages.VerificationReviewed event = json.readValue(message, Messages.VerificationReviewed.class);
        if (event.approved()) {
            notifier.tell(event.userId(), event.eventId(), "VERIFIED", "You're verified",
                    "Your identity is confirmed, and your limits are now higher.", "/verification", true);
        } else {
            notifier.tell(event.userId(), event.eventId(), "VERIFICATION_REJECTED", "Verification not accepted",
                    "We couldn't accept your document. Reason: " + event.reason() + " Please upload a new one.",
                    "/verification", true);
        }
    }

    @KafkaListener(topics = Topics.STATEMENT_READY)
    void statementReady(String message) {
        Messages.StatementReady event = json.readValue(message, Messages.StatementReady.class);
        notifier.tell(event.userId(), event.eventId(), "STATEMENT_READY", "Your statement is ready",
                "Your " + event.currency() + " statement for " + event.month() + " is ready to download.",
                "/wallets/" + event.walletId(), false);
    }

    private static String describe(String kind) {
        return switch (kind) {
            case "WITHDRAWAL" -> "withdrawal";
            case "TRANSFER" -> "transfer";
            default -> "payment";
        };
    }
}
