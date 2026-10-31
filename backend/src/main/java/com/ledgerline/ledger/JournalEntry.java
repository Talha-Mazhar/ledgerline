package com.ledgerline.ledger;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A movement of money to record in the ledger: two or more legs that add up to zero in each
 * currency. A deposit of 100 EUR is {@code -100 EUR} on the funding account and {@code +100 EUR}
 * on the wallet's account. The constructor refuses an entry that doesn't balance, so an unbalanced
 * entry can't even be built.
 *
 * @param initiatedBy the user who asked for the movement
 * @param idempotencyKey the client's key for this request; a retry with the same key and the same
 *     request returns the first result instead of moving the money again
 * @param fingerprint a description of the request (what, which wallet, how much) used to tell a
 *     genuine retry from a different request that reuses a key
 */
public record JournalEntry(
        EntryKind kind, UUID initiatedBy, String idempotencyKey, String fingerprint, List<Leg> legs) {

    /** One account's side of the entry: positive adds to its balance, negative takes from it. */
    public record Leg(UUID accountId, Money amount, String memo) {

        public Leg {
            Objects.requireNonNull(accountId, "accountId");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(memo, "memo");
            if (amount.amount().signum() == 0) {
                throw new IllegalArgumentException("A leg must move money; zero is not allowed");
            }
        }
    }

    public JournalEntry {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(initiatedBy, "initiatedBy");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(fingerprint, "fingerprint");
        legs = List.copyOf(legs);
        if (legs.size() < 2) {
            throw new IllegalArgumentException("A journal entry needs at least two legs");
        }
        Map<CurrencyCode, BigDecimal> totals = new EnumMap<>(CurrencyCode.class);
        for (Leg leg : legs) {
            totals.merge(leg.amount().currency(), leg.amount().amount(), BigDecimal::add);
        }
        totals.forEach((currency, total) -> {
            if (total.signum() != 0) {
                throw new IllegalArgumentException("The entry does not balance in " + currency + ": " + total);
            }
        });
    }

    /** Every account the entry touches, in leg order. */
    public Set<UUID> accountIds() {
        Set<UUID> ids = new LinkedHashSet<>();
        legs.forEach(leg -> ids.add(leg.accountId()));
        return ids;
    }
}
