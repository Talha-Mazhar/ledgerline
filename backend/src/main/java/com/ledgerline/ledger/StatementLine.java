package com.ledgerline.ledger;

import java.time.Instant;
import java.util.UUID;

/**
 * One posting as it appears on an account's statement.
 *
 * @param amount positive for money in, negative for money out
 * @param balanceAfter the account's balance right after this posting
 */
public record StatementLine(
        UUID postingId,
        UUID entryId,
        EntryKind kind,
        UUID accountId,
        Money amount,
        Money balanceAfter,
        String memo,
        Instant postedAt) {}
