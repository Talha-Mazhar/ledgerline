package com.ledgerline.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A user who can sign in. The password is only ever held as a hash. */
@Entity
@Table(name = "users")
class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private @Nullable Long version;

    /** For JPA only. */
    @SuppressWarnings("NullAway.Init")
    protected UserAccount() {}

    private UserAccount(UUID id, String email, String displayName, String passwordHash, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    static UserAccount register(String email, String displayName, String passwordHash, Instant now) {
        return new UserAccount(
                UUID.randomUUID(), normalise(email), displayName.trim(), passwordHash, now.truncatedTo(ChronoUnit.MICROS));
    }

    /** Emails are compared in lower case, without surrounding spaces. */
    static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    UUID id() {
        return id;
    }

    String email() {
        return email;
    }

    String displayName() {
        return displayName;
    }

    String passwordHash() {
        return passwordHash;
    }

    Instant createdAt() {
        return createdAt;
    }

    @Nullable Long version() {
        return version;
    }

    UserSummary summary() {
        return new UserSummary(id, email, displayName);
    }
}
