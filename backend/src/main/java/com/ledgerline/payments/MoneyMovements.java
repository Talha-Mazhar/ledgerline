package com.ledgerline.payments;

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
 * Deposits and withdrawals: one journal entry between the user's wallet and one of Ledgerline's
 * own accounts.
 *
 * <ul>
 *   <li>Deposit of 100 EUR: funding account {@code -100}, wallet {@code +100}.
 *   <li>Withdrawal of 40 EUR: wallet {@code -40}, payout account {@code +40}; refused if the wallet
 *       holds less than 40.
 *   <li>A withdrawal the risk rules hold: wallet {@code -40}, hold account {@code +40}, until two
 *       staff approve (hold → payout) or one rejects (hold → wallet).
 * </ul>
 */
@Service
class MoneyMovements {

    private final WalletDirectory wallets;
    private final Ledger ledger;
    private final PaymentBook book;
    private final OutgoingScreening screening;

    MoneyMovements(WalletDirectory wallets, Ledger ledger, PaymentBook book, OutgoingScreening screening) {
        this.wallets = wallets;
        this.ledger = ledger;
        this.book = book;
        this.screening = screening;
    }

    @Transactional
    public StatementLine deposit(UUID userId, UUID walletId, String typedAmount, UUID idempotencyKey) {
        OwnedWallet wallet = wallets.requireOwned(userId, walletId);
        Money amount = Amounts.positive(typedAmount, wallet.currency());
        String fingerprint = fingerprint(PaymentKind.DEPOSIT, wallet, amount);
        Optional<Payment> earlier = book.earlier(userId, idempotencyKey, fingerprint);
        if (earlier.isPresent()) {
            return book.lineOf(earlier.get(), wallet.id());
        }

        UUID funding = ledger.systemAccount(SystemAccount.FUNDING, wallet.currency());
        PostedEntry posted = ledger.post(new JournalEntry(
                EntryKind.DEPOSIT,
                userId,
                idempotencyKey.toString(),
                fingerprint,
                List.of(
                        new Leg(funding, amount.negate(), "Card top-up"),
                        new Leg(wallet.id(), amount, "Added money"))));
        if (!posted.replayed()) {
            Payment payment = book.save(Payment.request(
                            PaymentKind.DEPOSIT,
                            userId,
                            idempotencyKey.toString(),
                            fingerprint,
                            null,
                            wallet.id(),
                            null,
                            amount,
                            book.now())
                    .recorded(posted.entryId()));
            book.publish(book.completed(payment));
        }
        return posted.lineFor(wallet.id());
    }

    @Transactional
    public StatementLine withdraw(UUID userId, UUID walletId, String typedAmount, UUID idempotencyKey) {
        OwnedWallet wallet = wallets.requireOwned(userId, walletId);
        Money amount = Amounts.positive(typedAmount, wallet.currency());
        String fingerprint = fingerprint(PaymentKind.WITHDRAWAL, wallet, amount);
        Optional<Payment> earlier = book.earlier(userId, idempotencyKey, fingerprint);
        if (earlier.isPresent()) {
            return book.lineOf(earlier.get(), wallet.id());
        }

        ScreeningDecision decision = screening.screen(userId, amount, null, book.now());
        PostedEntry posted;
        if (decision.hold()) {
            UUID hold = ledger.systemAccount(SystemAccount.HOLD, wallet.currency());
            posted = ledger.post(new JournalEntry(
                    EntryKind.HOLD,
                    userId,
                    idempotencyKey.toString(),
                    fingerprint,
                    List.of(
                            new Leg(wallet.id(), amount.negate(), "Withdrawal to bank, awaiting approval"),
                            new Leg(hold, amount, "Held: withdrawal"))));
        } else {
            UUID payout = ledger.systemAccount(SystemAccount.PAYOUT, wallet.currency());
            posted = ledger.post(new JournalEntry(
                    EntryKind.WITHDRAWAL,
                    userId,
                    idempotencyKey.toString(),
                    fingerprint,
                    List.of(
                            new Leg(wallet.id(), amount.negate(), "Withdrawal to bank"),
                            new Leg(payout, amount, "Payout to user bank"))));
        }
        if (!posted.replayed()) {
            Payment payment = Payment.request(
                    PaymentKind.WITHDRAWAL,
                    userId,
                    idempotencyKey.toString(),
                    fingerprint,
                    wallet.id(),
                    null,
                    null,
                    amount,
                    book.now());
            if (decision.hold()) {
                book.publish(
                        book.held(book.save(payment.held(posted.entryId(), String.join(" ", decision.reasons())))));
            } else {
                book.publish(book.completed(book.save(payment.recorded(posted.entryId()))));
            }
        }
        return posted.lineFor(wallet.id());
    }

    /** What makes two requests "the same request" for idempotency: kind, wallet and amount. */
    private static String fingerprint(PaymentKind kind, OwnedWallet wallet, Money amount) {
        return kind + "|" + wallet.id() + "|" + amount.toPlainString() + "|" + amount.currency();
    }
}
