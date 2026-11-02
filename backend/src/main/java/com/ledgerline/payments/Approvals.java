package com.ledgerline.payments;

import com.ledgerline.identity.UserDirectory;
import com.ledgerline.ledger.EntryKind;
import com.ledgerline.ledger.JournalEntry;
import com.ledgerline.ledger.JournalEntry.Leg;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.SystemAccount;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The four-eyes rule for held payments: two different staff members approve, or one rejects.
 *
 * <p>Each decision locks the payment row ({@code SELECT ... FOR UPDATE}) first, so two staff
 * clicking at the same moment are handled one after the other: the second sees the first's
 * decision instead of overwriting it.
 */
@Service
class Approvals {

    private final PaymentRepository payments;
    private final Ledger ledger;
    private final UserDirectory users;
    private final PaymentBook book;

    Approvals(PaymentRepository payments, Ledger ledger, UserDirectory users, PaymentBook book) {
        this.payments = payments;
        this.ledger = ledger;
        this.users = users;
        this.book = book;
    }

    @Transactional
    public Payment approve(UUID staff, UUID paymentId) {
        Payment payment = payments.lockById(paymentId).orElseThrow(PaymentNotFoundException::new);
        boolean released = payment.approve(staff, book.now());
        book.publish(new PaymentApproved(UUID.randomUUID(), payment.id(), staff, released, book.now()));
        if (released) {
            settle(payment);
            book.count(payment);
            book.publish(book.completed(payment));
        }
        return payment;
    }

    @Transactional
    public Payment reject(UUID staff, UUID paymentId, String reason) {
        String why = reason.trim();
        if (why.isEmpty() || why.length() > 500) {
            throw new PaymentRefusedException("Give the customer a reason, up to 500 characters.");
        }
        Payment payment = payments.lockById(paymentId).orElseThrow(PaymentNotFoundException::new);
        payment.reject(staff, why, book.now());
        book.count(payment);
        Money amount = payment.amount();
        UUID hold = ledger.systemAccount(SystemAccount.HOLD, amount.currency());
        UUID wallet = Objects.requireNonNull(payment.fromWalletId(), "a held payment has a source wallet");
        ledger.post(new JournalEntry(
                EntryKind.RELEASE,
                payment.initiatedBy(),
                "payment-" + payment.id() + "-release",
                "RELEASE|" + payment.id(),
                List.of(
                        new Leg(hold, amount.negate(), "Released: not approved"),
                        new Leg(wallet, amount, "Returned: " + what(payment) + " was not approved"))));
        book.publish(new PaymentRejected(
                UUID.randomUUID(),
                payment.id(),
                payment.kind(),
                payment.initiatedBy(),
                staff,
                why,
                amount.toPlainString(),
                amount.currency(),
                book.now()));
        return payment;
    }

    /** Moves held money on to where it was going: the recipient's wallet, or out to the bank. */
    private void settle(Payment payment) {
        Money amount = payment.amount();
        UUID hold = ledger.systemAccount(SystemAccount.HOLD, amount.currency());
        String key = "payment-" + payment.id() + "-settle";
        String fingerprint = "SETTLE|" + payment.id();
        if (payment.kind() == PaymentKind.TRANSFER) {
            UUID to = Objects.requireNonNull(payment.toWalletId(), "a transfer has a destination wallet");
            String sender = users.require(payment.initiatedBy()).displayName();
            ledger.post(new JournalEntry(
                    EntryKind.TRANSFER,
                    payment.initiatedBy(),
                    key,
                    fingerprint,
                    List.of(
                            new Leg(hold, amount.negate(), "Released: approved transfer"),
                            new Leg(to, amount, "Received from " + sender))));
        } else {
            UUID payout = ledger.systemAccount(SystemAccount.PAYOUT, amount.currency());
            ledger.post(new JournalEntry(
                    EntryKind.WITHDRAWAL,
                    payment.initiatedBy(),
                    key,
                    fingerprint,
                    List.of(
                            new Leg(hold, amount.negate(), "Released: approved withdrawal"),
                            new Leg(payout, amount, "Payout to user bank"))));
        }
    }

    private String what(Payment payment) {
        UUID counterparty = payment.counterpartyId();
        if (payment.kind() == PaymentKind.TRANSFER && counterparty != null) {
            return "transfer to " + users.require(counterparty).displayName();
        }
        return "withdrawal";
    }
}
