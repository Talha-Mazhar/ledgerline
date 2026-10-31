package com.ledgerline.wallets;

import com.ledgerline.ledger.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A user's wallet in one currency.
 *
 * <p>The wallet holds no balance column. Money lives in the ledger as journal entries, and the
 * balance is derived from them, so there is no second copy of the truth to drift out of sync.
 *
 * <p>The id is generated here rather than by the database, so a wallet has its identity before it
 * is saved. The {@code version} column gives optimistic locking, and because it is {@code null}
 * until the first save, Spring Data uses it to tell new wallets from existing ones without an
 * extra query.
 */
@Entity
@Table(name = "wallets")
class Wallet {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WalletStatus status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Version
    private @Nullable Long version;

    /** For JPA only, which sets the fields itself after constructing. */
    @SuppressWarnings("NullAway.Init")
    protected Wallet() {}

    private Wallet(UUID id, UUID ownerId, CurrencyCode currency, Instant openedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.currency = currency;
        this.status = WalletStatus.ACTIVE;
        this.openedAt = openedAt;
    }

    /** Opens a new, active wallet. PostgreSQL stores microseconds, so the time is cut to match. */
    static Wallet open(UUID ownerId, CurrencyCode currency, Instant now) {
        return new Wallet(UUID.randomUUID(), ownerId, currency, now.truncatedTo(ChronoUnit.MICROS));
    }

    UUID id() {
        return id;
    }

    UUID ownerId() {
        return ownerId;
    }

    CurrencyCode currency() {
        return currency;
    }

    WalletStatus status() {
        return status;
    }

    Instant openedAt() {
        return openedAt;
    }

    @Nullable Long version() {
        return version;
    }
}
