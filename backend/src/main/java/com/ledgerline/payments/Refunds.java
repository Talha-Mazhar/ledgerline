package com.ledgerline.payments;

import com.ledgerline.identity.UserDirectory;
import com.ledgerline.ledger.EntryKind;
import com.ledgerline.ledger.JournalEntry;
import com.ledgerline.ledger.JournalEntry.Leg;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.PostedEntry;
import com.ledgerline.ledger.SystemAccount;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refunds, done by staff: a new, reversing entry. The original entry is never changed (the ledger
 * is append-only); the original payment becomes REFUNDED and a REFUND payment records who did it.
 *
 * <ul>
 *   <li>A transfer: the recipient's wallet pays the amount back to the sender's. Refused if the
 *       recipient no longer has that much.
 *   <li>A top-up: the wallet pays the amount back to the card (the funding account).
 * </ul>
 */
@Service
class Refunds {

    private final PaymentRepository payments;
    private final Ledger ledger;
    private final UserDirectory users;
    private final PaymentBook book;

    Refunds(PaymentRepository payments, Ledger ledger, UserDirectory users, PaymentBook book) {
        this.payments = payments;
        this.ledger = ledger;
        this.users = users;
        this.book = book;
    }

    @Transactional
    public Payment refund(UUID staff, UUID paymentId) {
        Payment original = payments.lockById(paymentId).orElseThrow(PaymentNotFoundException::new);
        if (original.kind() != PaymentKind.TRANSFER && original.kind() != PaymentKind.DEPOSIT) {
            throw new PaymentRefusedException("Only transfers and top-ups can be refunded.");
        }
        original.markRefunded(book.now());

        Money amount = original.amount();
        UUID payee = Objects.requireNonNull(original.toWalletId(), "transfers and top-ups have a destination wallet");
        List<Leg> legs;
        UUID backTo;
        if (original.kind() == PaymentKind.TRANSFER) {
            backTo = Objects.requireNonNull(original.fromWalletId(), "a transfer has a source wallet");
            String sender = users.require(original.initiatedBy()).displayName();
            String recipient = users.require(Objects.requireNonNull(original.counterpartyId())).displayName();
            legs = List.of(new Leg(payee, amount.negate(), "Refund to " + sender), new Leg(backTo, amount, "Refund from " + recipient));
        } else {
            backTo = ledger.systemAccount(SystemAccount.FUNDING, amount.currency());
            legs = List.of(new Leg(payee, amount.negate(), "Top-up refunded to card"), new Leg(backTo, amount, "Card refund"));
        }
        String key = "refund-" + original.id();
        PostedEntry posted = ledger.post(new JournalEntry(EntryKind.REFUND, staff, key, "REFUND|" + original.id(), legs));

        Payment refund = book.save(Payment.request(
                        PaymentKind.REFUND,
                        staff,
                        key,
                        "REFUND|" + original.id(),
                        payee,
                        original.kind() == PaymentKind.TRANSFER ? backTo : null,
                        original.initiatedBy(),
                        amount,
                        book.now())
                .recorded(posted.entryId())
                .refunding(original.id()));
        book.publish(new PaymentRefunded(
                UUID.randomUUID(),
                original.id(),
                refund.id(),
                original.kind(),
                staff,
                original.initiatedBy(),
                original.counterpartyId(),
                amount.toPlainString(),
                amount.currency(),
                book.now()));
        return refund;
    }
}
