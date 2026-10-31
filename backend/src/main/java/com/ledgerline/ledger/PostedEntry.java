package com.ledgerline.ledger;

import java.util.List;
import java.util.UUID;

/**
 * The result of {@link Ledger#post}: the entry's id and its lines.
 *
 * @param replayed true when the request was a retry and these are the lines recorded the first time
 */
public record PostedEntry(UUID entryId, boolean replayed, List<StatementLine> lines) {

    public PostedEntry {
        lines = List.copyOf(lines);
    }

    /** The line for one account, e.g. the wallet the user is looking at. */
    public StatementLine lineFor(UUID accountId) {
        return lines.stream()
                .filter(line -> line.accountId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The entry has no line for account " + accountId));
    }
}
