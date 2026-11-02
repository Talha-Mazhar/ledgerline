package com.ledgerline.payments;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A payment a user asked for, and its state. The money itself is in the ledger; {@code entryId}
 * points at the first journal entry the payment recorded (the movement, or the hold).
 *
 * <p>Every state change goes through {@link #moveTo}, which applies {@link PaymentStatus}'s state
 * machine, so an illegal step fails here whatever code path asked for it.
 */
@Entity
@Table(name = "payments")
class Payment {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 256)
    private String fingerprint;

    @Column(name = "from_wallet_id")
    private @Nullable UUID fromWalletId;

    @Column(name = "to_wallet_id")
    private @Nullable UUID toWalletId;

    @Column(name = "counterparty_id")
    private @Nullable UUID counterpartyId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "hold_reason", length = 500)
    private @Nullable String holdReason;

    @Column(name = "refund_of")
    private @Nullable UUID refundOf;

    @Column(name = "first_approver")
    private @Nullable UUID firstApprover;

    @Column(name = "second_approver")
    private @Nullable UUID secondApprover;

    @Column(name = "rejected_by")
    private @Nullable UUID rejectedBy;

    @Column(name = "rejection_reason", length = 500)
    private @Nullable String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private @Nullable Long version;

    /** For JPA only. */
    @SuppressWarnings("NullAway.Init")
    protected Payment() {}

    private Payment(
            PaymentKind kind,
            PaymentStatus status,
            UUID initiatedBy,
            String idempotencyKey,
            String fingerprint,
            Money amount,
            UUID entryId,
            Instant now) {
        this.id = UUID.randomUUID();
        this.kind = kind;
        this.status = status;
        this.initiatedBy = initiatedBy;
        this.idempotencyKey = idempotencyKey;
        this.fingerprint = fingerprint;
        this.amount = amount.amount();
        this.currency = amount.currency();
        this.entryId = entryId;
        this.createdAt = now.truncatedTo(ChronoUnit.MICROS);
        this.updatedAt = this.createdAt;
    }

    /** What the user asked for; the ledger entry that recorded it is set by {@link #recorded}. */
    static Payment request(
            PaymentKind kind,
            UUID initiatedBy,
            String idempotencyKey,
            String fingerprint,
            @Nullable UUID fromWallet,
            @Nullable UUID toWallet,
            @Nullable UUID counterparty,
            Money amount,
            Instant now) {
        Payment payment = new Payment(
                kind, PaymentStatus.COMPLETED, initiatedBy, idempotencyKey, fingerprint, amount, new UUID(0, 0), now);
        payment.fromWalletId = fromWallet;
        payment.toWalletId = toWallet;
        payment.counterpartyId = counterparty;
        return payment;
    }

    /** The money moved at once. */
    Payment recorded(UUID entry) {
        this.entryId = entry;
        this.status = PaymentStatus.COMPLETED;
        return this;
    }

    /** The money is held until two staff approve. */
    Payment held(UUID holdEntry, String reasons) {
        this.entryId = holdEntry;
        this.status = PaymentStatus.HELD;
        this.holdReason = reasons;
        return this;
    }

    /** A refund of another payment. */
    Payment refunding(UUID originalPayment) {
        this.refundOf = originalPayment;
        return this;
    }

    /**
     * Records one staff approval. The first moves the payment on to a second, different person;
     * the second completes it.
     *
     * @return true when this was the final approval and the money should now be released
     */
    boolean approve(UUID staff, Instant now) {
        if (staff.equals(initiatedBy)) {
            throw new PaymentRefusedException("You can't approve your own payment.");
        }
        if (status == PaymentStatus.HELD) {
            moveTo(PaymentStatus.AWAITING_SECOND_APPROVAL, now);
            this.firstApprover = staff;
            return false;
        }
        if (status == PaymentStatus.AWAITING_SECOND_APPROVAL) {
            if (staff.equals(firstApprover)) {
                throw new PaymentRefusedException("A second, different person has to approve this payment.");
            }
            moveTo(PaymentStatus.COMPLETED, now);
            this.secondApprover = staff;
            return true;
        }
        throw new PaymentRefusedException("This payment isn't waiting for approval.");
    }

    void reject(UUID staff, String reason, Instant now) {
        if (!status.waitingForApproval()) {
            throw new PaymentRefusedException("This payment isn't waiting for approval.");
        }
        moveTo(PaymentStatus.REJECTED, now);
        this.rejectedBy = staff;
        this.rejectionReason = reason;
    }

    void markRefunded(Instant now) {
        if (status != PaymentStatus.COMPLETED) {
            throw new PaymentRefusedException("Only a completed payment can be refunded.");
        }
        moveTo(PaymentStatus.REFUNDED, now);
    }

    private void moveTo(PaymentStatus next, Instant now) {
        if (!status.canMoveTo(next)) {
            throw new PaymentRefusedException("A " + status + " payment can't become " + next + ".");
        }
        this.status = next;
        this.updatedAt = now.truncatedTo(ChronoUnit.MICROS);
    }

    UUID id() {
        return id;
    }

    PaymentKind kind() {
        return kind;
    }

    PaymentStatus status() {
        return status;
    }

    UUID initiatedBy() {
        return initiatedBy;
    }

    String fingerprint() {
        return fingerprint;
    }

    @Nullable UUID fromWalletId() {
        return fromWalletId;
    }

    @Nullable UUID toWalletId() {
        return toWalletId;
    }

    @Nullable UUID counterpartyId() {
        return counterpartyId;
    }

    Money amount() {
        return new Money(amount, currency);
    }

    UUID entryId() {
        return entryId;
    }

    @Nullable String holdReason() {
        return holdReason;
    }

    @Nullable UUID refundOf() {
        return refundOf;
    }

    @Nullable UUID firstApprover() {
        return firstApprover;
    }

    @Nullable UUID secondApprover() {
        return secondApprover;
    }

    @Nullable UUID rejectedBy() {
        return rejectedBy;
    }

    @Nullable String rejectionReason() {
        return rejectionReason;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    @Nullable Long version() {
        return version;
    }
}
