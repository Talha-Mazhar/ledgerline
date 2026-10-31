package com.ledgerline.payments;

import com.ledgerline.identity.UserDirectory;
import com.ledgerline.identity.UserSummary;
import com.ledgerline.ledger.EntryKind;
import com.ledgerline.ledger.JournalEntry;
import com.ledgerline.ledger.JournalEntry.Leg;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.StatementLine;
import com.ledgerline.wallets.OwnedWallet;
import com.ledgerline.wallets.WalletDirectory;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sending money to another Ledgerline user, found by email.
 *
 * <p>Sending 30 EUR to Jana is one journal entry with two legs: the sender's EUR wallet {@code -30},
 * Jana's EUR wallet {@code +30}. Both lines are written in the same transaction, so the money is
 * never in neither wallet or in both. Each side sees its own memo ("Sent to Jana Nováková",
 * "Received from Demo user").
 *
 * <p>The money stays in its currency: Jana needs a wallet in the currency sent.
 */
@Service
class Transfers {

    /** A completed transfer: the sender's new line and who received the money. */
    record Sent(StatementLine line, UserSummary recipient) {}

    private final WalletDirectory wallets;
    private final UserDirectory users;
    private final Ledger ledger;

    Transfers(WalletDirectory wallets, UserDirectory users, Ledger ledger) {
        this.wallets = wallets;
        this.users = users;
        this.ledger = ledger;
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
        UserSummary sender = users.require(userId);

        JournalEntry entry = new JournalEntry(
                EntryKind.TRANSFER,
                userId,
                idempotencyKey.toString(),
                EntryKind.TRANSFER + "|" + from.id() + "|" + to.id() + "|" + amount.toPlainString() + "|"
                        + amount.currency(),
                List.of(
                        new Leg(from.id(), amount.negate(), "Sent to " + recipient.displayName()),
                        new Leg(to.id(), amount, "Received from " + sender.displayName())));
        return new Sent(ledger.post(entry).lineFor(from.id()), recipient);
    }
}
