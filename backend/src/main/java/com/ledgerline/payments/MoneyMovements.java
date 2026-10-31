package com.ledgerline.payments;

import com.ledgerline.ledger.EntryKind;
import com.ledgerline.ledger.JournalEntry;
import com.ledgerline.ledger.JournalEntry.Leg;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.StatementLine;
import com.ledgerline.ledger.SystemAccount;
import com.ledgerline.wallets.OwnedWallet;
import com.ledgerline.wallets.WalletDirectory;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deposits and withdrawals. Each builds one journal entry between the user's wallet and one of
 * Ledgerline's own accounts, and the ledger records it.
 *
 * <ul>
 *   <li>Deposit of 100 EUR: funding account {@code -100}, wallet {@code +100}.
 *   <li>Withdrawal of 40 EUR: wallet {@code -40}, payout account {@code +40}. The ledger refuses it
 *       if the wallet holds less than 40.
 * </ul>
 */
@Service
class MoneyMovements {

    private final WalletDirectory wallets;
    private final Ledger ledger;

    MoneyMovements(WalletDirectory wallets, Ledger ledger) {
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Transactional
    public StatementLine deposit(UUID userId, UUID walletId, String typedAmount, UUID idempotencyKey) {
        OwnedWallet wallet = wallets.requireOwned(userId, walletId);
        Money amount = Amounts.positive(typedAmount, wallet.currency());
        UUID funding = ledger.systemAccount(SystemAccount.FUNDING, wallet.currency());
        JournalEntry entry = new JournalEntry(
                EntryKind.DEPOSIT,
                userId,
                idempotencyKey.toString(),
                fingerprint(EntryKind.DEPOSIT, wallet, amount),
                List.of(new Leg(funding, amount.negate(), "Card top-up"), new Leg(wallet.id(), amount, "Added money")));
        return ledger.post(entry).lineFor(wallet.id());
    }

    @Transactional
    public StatementLine withdraw(UUID userId, UUID walletId, String typedAmount, UUID idempotencyKey) {
        OwnedWallet wallet = wallets.requireOwned(userId, walletId);
        Money amount = Amounts.positive(typedAmount, wallet.currency());
        UUID payout = ledger.systemAccount(SystemAccount.PAYOUT, wallet.currency());
        JournalEntry entry = new JournalEntry(
                EntryKind.WITHDRAWAL,
                userId,
                idempotencyKey.toString(),
                fingerprint(EntryKind.WITHDRAWAL, wallet, amount),
                List.of(
                        new Leg(wallet.id(), amount.negate(), "Withdrawal to bank"),
                        new Leg(payout, amount, "Payout to user bank")));
        return ledger.post(entry).lineFor(wallet.id());
    }

    /** What makes two requests "the same request" for idempotency: kind, wallet and amount. */
    private static String fingerprint(EntryKind kind, OwnedWallet wallet, Money amount) {
        return kind + "|" + wallet.id() + "|" + amount.toPlainString() + "|" + amount.currency();
    }
}
