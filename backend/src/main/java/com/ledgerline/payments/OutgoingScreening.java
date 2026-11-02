package com.ledgerline.payments;

import com.ledgerline.ledger.Money;
import com.ledgerline.risk.PaymentScreening;
import com.ledgerline.risk.ScreeningDecision;
import com.ledgerline.risk.ScreeningRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Asks the risk module about an outgoing payment, giving it the customer's recent history from
 * this module's own table. Risk decides; payments only reports.
 */
@Component
class OutgoingScreening {

    private final PaymentRepository payments;
    private final PaymentScreening screening;

    OutgoingScreening(PaymentRepository payments, PaymentScreening screening) {
        this.payments = payments;
        this.screening = screening;
    }

    ScreeningDecision screen(UUID customer, Money amount, @Nullable UUID recipient, Instant now) {
        List<Payment> lastDay = payments.outgoingSince(customer, now.minus(Duration.ofHours(24)));
        Instant windowStart = now.minus(screening.velocityWindow());
        int inWindow = (int) lastDay.stream()
                .filter(payment -> !payment.createdAt().isBefore(windowStart))
                .count();
        boolean newRecipient = recipient != null
                && !payments.existsByInitiatedByAndCounterpartyIdAndStatusIn(
                        customer, recipient, EnumSet.complementOf(EnumSet.of(PaymentStatus.REJECTED)));
        List<Money> sent = lastDay.stream().map(Payment::amount).toList();
        return screening.screen(new ScreeningRequest(customer, amount, sent, inWindow, newRecipient));
    }
}
