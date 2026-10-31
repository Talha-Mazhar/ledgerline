package com.ledgerline.wallets;

import com.ledgerline.ledger.StatementLine;
import java.time.Instant;
import java.util.UUID;

/**
 * The contract's {@code ActivityItem}: one line of a wallet's ledger, as JSON. Public because the
 * payments module answers a deposit or withdrawal with the wallet's new line.
 */
public record ActivityItemResponse(
        UUID id,
        UUID entryId,
        UUID walletId,
        ActivityKind kind,
        String description,
        MoneyResponse amount,
        MoneyResponse balanceAfter,
        Instant occurredAt) {

    /** A wallet's ledger line. The wallet's id is its ledger account's id. */
    public static ActivityItemResponse from(StatementLine line) {
        return new ActivityItemResponse(
                line.postingId(),
                line.entryId(),
                line.accountId(),
                ActivityKind.of(line),
                line.memo(),
                MoneyResponse.from(line.amount()),
                MoneyResponse.from(line.balanceAfter()),
                line.postedAt());
    }
}
