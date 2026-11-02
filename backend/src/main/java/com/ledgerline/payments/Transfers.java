package com.ledgerline.payments;

import com.ledgerline.identity.UserDirectory;
import com.ledgerline.identity.UserSummary;
import com.ledgerline.ledger.EntryKind;
import com.ledgerline.ledger.JournalEntry;
import com.ledgerline.ledger.JournalEntry.Leg;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.PostedEntry;
import com.ledgerline.ledger.StatementLine;
import com.ledgerline.ledger.SystemAccount;
import com.ledgerline.risk.ScreeningDecision;
import com.ledgerline.wallets.OwnedWallet;
import com.ledgerline.wallets.WalletDirectory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sending money to another Ledgerline user, found by email.
 *
 * <p>Sending 30 EUR to Jana is one journal entry: the sender's EUR wallet {@code -30}, Jana's EUR
 * wallet {@code +30}. Each side sees its own memo. If the risk rules hold the transfer, the money
 * goes from the sender's wallet to Ledgerline's hold account instead, and only reaches Jana after
 * two different staff approve.
 *
 * <p>The money stays in its currency: Jana needs a wallet in the currency sent.
 */
@Service
class Transfers {

    /** A transfer as the sender sees it: their new line, the recipient, and the payment's state. */
    record Sent(StatementLine line, UserSummary recipient, Payment payment) {}

    private final WalletDirectory wallets;
    private final UserDirectory users;
    private final Ledger ledger;
    private final PaymentBook book;
    private final OutgoingScreening screening;

    Transfers(
            WalletDirectory wallets,
            UserDirectory users,
            Ledger ledger,
            PaymentBook book,
            OutgoingScreening screening) {
        this.wallets = wallets;
        this.users = users;
        this.ledger = ledger;
        this.book = book;
        this.screening = screening;
    }

    @Transactional
    public Sent send(UUID userId, UUID fromWalletId, String recipientEmail, String typedAmount, UUID idempotencyKey) {
        OwnedWallet from = wallets.requireOwned(userId, fromWalletId);
        Money amount = Amounts.positive(typedAmount, from.currency());
        UserSummary recipient =
                users.findByEmail(recipientEmail).orElseThrow(TransferRefusedException::unknownRecipient);
        if (recipient.id().equals(userId)) {
            throw TransferRefusedException.toSelf();
        }
        OwnedWallet to = wallets.findByOwnerAndCurrency(recipient.id(), from.currency())
                .orElseThrow(() -> TransferRefusedException.noWallet(
                        recipient.displayName(), from.currency().name()));
        String fingerprint =
                "TRANSFER|" + from.id() + "|" + to.id() + "|" + amount.toPlainString() + "|" + amount.currency();
        Optional<Payment> earlier = book.earlier(userId, idempotencyKey, fingerprint);
        if (earlier.isPresent()) {
            return new Sent(book.lineOf(earlier.get(), from.id()), recipient, earlier.get());
        }

        UserSummary sender = users.require(userId);
        ScreeningDecision decision = screening.screen(userId, amount, recipient.id(), book.now());
        PostedEntry posted;
        if (decision.hold()) {
            UUID hold = ledger.systemAccount(SystemAccount.HOLD, from.currency());
            posted = ledger.post(new JournalEntry(
                    EntryKind.HOLD,
                    userId,
                    idempotencyKey.toString(),
                    fingerprint,
                    List.of(
                            new Leg(
                                    from.id(),
                                    amount.negate(),
                                    "Sent to " + recipient.displayName() + ", awaiting approval"),
                            new Leg(hold, amount, "Held: transfer to " + recipient.displayName()))));
        } else {
            posted = ledger.post(new JournalEntry(
                    EntryKind.TRANSFER,
                    userId,
                    idempotencyKey.toString(),
                    fingerprint,
                    List.of(
                            new Leg(from.id(), amount.negate(), "Sent to " + recipient.displayName()),
                            new Leg(to.id(), amount, "Received from " + sender.displayName()))));
        }
        if (posted.replayed()) {
            return new Sent(
                    posted.lineFor(from.id()), recipient, book.afterReplay(userId, idempotencyKey, fingerprint));
        }

        Payment payment = Payment.request(
                PaymentKind.TRANSFER,
                userId,
                idempotencyKey.toString(),
                fingerprint,
                from.id(),
                to.id(),
                recipient.id(),
                amount,
                book.now());
        if (decision.hold()) {
            payment = book.save(payment.held(posted.entryId(), String.join(" ", decision.reasons())));
            book.publish(book.held(payment));
        } else {
            payment = book.save(payment.recorded(posted.entryId()));
            book.publish(book.completed(payment));
        }
        return new Sent(posted.lineFor(from.id()), recipient, payment);
    }
}
