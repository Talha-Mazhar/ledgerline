package com.ledgerline.onboarding;

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

/** One customer's identity verification. Re-submitting after a rejection replaces the document. */
@Entity
@Table(name = "verifications")
class Verification {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private VerificationStatus status;

    @Column(name = "document_key", nullable = false, length = 200)
    private String documentKey;

    @Column(name = "document_name", nullable = false, length = 200)
    private String documentName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_by")
    private @Nullable UUID reviewedBy;

    @Column(name = "reviewed_at")
    private @Nullable Instant reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private @Nullable String rejectionReason;

    @Version
    private @Nullable Long version;

    /** For JPA only. */
    @SuppressWarnings("NullAway.Init")
    protected Verification() {}

    Verification(UUID userId) {
        this.userId = userId;
        this.status = VerificationStatus.PENDING_REVIEW;
        this.documentKey = "";
        this.documentName = "";
        this.contentType = "";
        this.submittedAt = Instant.EPOCH;
    }

    /** Records a newly uploaded document; any earlier review is cleared. */
    void submit(String key, String name, String type, Instant now) {
        if (status == VerificationStatus.VERIFIED) {
            throw new VerificationRefusedException("You're already verified.");
        }
        this.documentKey = key;
        this.documentName = name;
        this.contentType = type;
        this.submittedAt = now.truncatedTo(ChronoUnit.MICROS);
        this.status = VerificationStatus.PENDING_REVIEW;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.rejectionReason = null;
    }

    void approve(UUID reviewer, Instant now) {
        review(reviewer, now);
        this.status = VerificationStatus.VERIFIED;
    }

    void reject(UUID reviewer, String reason, Instant now) {
        review(reviewer, now);
        this.status = VerificationStatus.REJECTED;
        this.rejectionReason = reason;
    }

    private void review(UUID reviewer, Instant now) {
        if (status != VerificationStatus.PENDING_REVIEW) {
            throw new VerificationRefusedException("This verification isn't waiting for review.");
        }
        if (reviewer.equals(userId)) {
            throw new VerificationRefusedException("Staff can't review their own verification.");
        }
        this.reviewedBy = reviewer;
        this.reviewedAt = now.truncatedTo(ChronoUnit.MICROS);
    }

    UUID userId() {
        return userId;
    }

    VerificationStatus status() {
        return status;
    }

    String documentKey() {
        return documentKey;
    }

    String documentName() {
        return documentName;
    }

    String contentType() {
        return contentType;
    }

    Instant submittedAt() {
        return submittedAt;
    }

    @Nullable Instant reviewedAt() {
        return reviewedAt;
    }

    @Nullable String rejectionReason() {
        return rejectionReason;
    }

    @Nullable Long version() {
        return version;
    }
}
