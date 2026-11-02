package com.ledgerline.ledger;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ledger module's public API: the only way other modules read balances or move money.
 *
 * <p>Every method runs in the caller's transaction when there is one, so opening a wallet and its
 * ledger account, or a payment and its entry, commit or roll back together.
 */
public interface Ledger {

    /** Opens a user's account. Its id is chosen by the caller: a wallet's account shares its id. */
    void openUserAccount(UUID accountId, UUID ownerId, CurrencyCode currency);

    /** The id of one of Ledgerline's own accounts in a currency. */
    UUID systemAccount(SystemAccount account, CurrencyCode currency);

    /** The account's current balance. */
    Money balanceOf(UUID accountId);

    /** Current balances of several accounts in one query. */
    Map<UUID, Money> balancesOf(Collection<UUID> accountIds);

    /**
     * Records a journal entry, all legs or none.
     *
     * @throws InsufficientFundsException if a user account would go below zero
     * @throws IdempotencyKeyReusedException if the key was used before for a different request
     */
    PostedEntry post(JournalEntry entry);

    /** A recorded entry and its lines, e.g. to answer a retried request the same way again. */
    PostedEntry entry(UUID entryId);

    /** An account's lines from {@code from} (inclusive) to {@code to} (exclusive), oldest first. */
    List<StatementLine> linesBetween(UUID accountId, Instant from, Instant to);

    /** Several accounts' lines in a period, oldest first, at most {@code limit} of them. */
    List<StatementLine> linesBetween(Collection<UUID> accountIds, Instant from, Instant to, int limit);

    /** An account's balance just before a moment: what a statement for a period opens with. */
    Money balanceBefore(UUID accountId, Instant moment);

    /** The latest lines of the given accounts, newest first. */
    List<StatementLine> statement(Collection<UUID> accountIds, int limit);
}
