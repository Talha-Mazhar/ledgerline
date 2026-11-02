package com.ledgerline.wallets;

import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.StatementLine;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read-only access to a customer's balances and activity across all their wallets. */
public interface WalletReader {

    /** The balance of each of the owner's wallets, oldest wallet first. */
    List<Money> balancesOf(UUID ownerId);

    /** The owner's lines across all wallets from {@code from} (inclusive) to {@code to} (exclusive). */
    List<StatementLine> activityBetween(UUID ownerId, Instant from, Instant to, int limit);
}
