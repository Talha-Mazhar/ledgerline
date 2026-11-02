package com.ledgerline.onboarding;

import com.ledgerline.documents.DocumentStore;
import com.ledgerline.documents.DocumentStore.StoredDocument;
import com.ledgerline.identity.UserDirectory;
import com.ledgerline.identity.UserSummary;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Submitting and reviewing identity verifications. Also the module's {@link CustomerTiers}.
 *
 * <p>Uploaded files are checked by content, not just by name: the first bytes must be those of a
 * PDF, JPEG or PNG, whatever the file is called. The file goes to object storage before the row
 * is saved; if saving fails, an orphan file is harmless, while a row pointing at a missing file
 * would not be.
 */
@Service
class Verifications implements CustomerTiers {

    static final int MAX_BYTES = 5 * 1024 * 1024;

    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

    /** A verification as staff see it in the review queue. */
    record ReviewItem(Verification verification, UserSummary user) {}

    private final VerificationRepository verifications;
    private final DocumentStore documents;
    private final UserDirectory users;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    Verifications(
            VerificationRepository verifications,
            DocumentStore documents,
            UserDirectory users,
            ApplicationEventPublisher events,
            Clock clock) {
        this.verifications = verifications;
        this.documents = documents;
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Tier tierOf(UUID userId) {
        return verifications
                .findById(userId)
                .filter(verification -> verification.status() == VerificationStatus.VERIFIED)
                .map(verified -> Tier.VERIFIED)
                .orElse(Tier.BASIC);
    }

    @Transactional(readOnly = true)
    Optional<Verification> of(UUID userId) {
        return verifications.findById(userId);
    }

    @Transactional
    Verification submit(UUID userId, String fileName, byte[] content) {
        String contentType = detectType(content);
        if (content.length > MAX_BYTES) {
            throw new InvalidDocumentException("The file is larger than 5 MB.");
        }
        Verification verification = verifications.findById(userId).orElseGet(() -> new Verification(userId));
        String key = "kyc/" + userId + "/" + UUID.randomUUID() + extension(contentType);
        documents.put(key, content, contentType);
        verification.submit(key, cleanName(fileName), contentType, clock.instant());
        Verification saved = verifications.save(verification);
        events.publishEvent(new VerificationSubmitted(
                UUID.randomUUID(), userId, users.require(userId).displayName()));
        return saved;
    }

    @Transactional(readOnly = true)
    List<ReviewItem> waitingForReview() {
        return verifications.findByStatusOrderBySubmittedAt(VerificationStatus.PENDING_REVIEW).stream()
                .map(verification -> new ReviewItem(verification, users.require(verification.userId())))
                .toList();
    }

    @Transactional(readOnly = true)
    StoredDocument document(UUID userId) {
        Verification verification = verifications.findById(userId).orElseThrow(NoVerificationException::new);
        return documents.get(verification.documentKey()).orElseThrow(NoVerificationException::new);
    }

    @Transactional
    Verification approve(UUID reviewer, UUID userId) {
        Verification verification = verifications.findById(userId).orElseThrow(NoVerificationException::new);
        verification.approve(reviewer, clock.instant());
        events.publishEvent(new VerificationReviewed(UUID.randomUUID(), userId, reviewer, true, null));
        return verification;
    }

    @Transactional
    Verification reject(UUID reviewer, UUID userId, String reason) {
        String why = reason.trim();
        if (why.isEmpty() || why.length() > 500) {
            throw new InvalidDocumentException("Give the customer a reason, up to 500 characters.");
        }
        Verification verification = verifications.findById(userId).orElseThrow(NoVerificationException::new);
        verification.reject(reviewer, why, clock.instant());
        events.publishEvent(new VerificationReviewed(UUID.randomUUID(), userId, reviewer, false, why));
        return verification;
    }

    /** The real type of the file from its first bytes, or a refusal. */
    static String detectType(byte[] content) {
        if (startsWith(content, PDF)) {
            return "application/pdf";
        }
        if (startsWith(content, JPEG)) {
            return "image/jpeg";
        }
        if (startsWith(content, PNG)) {
            return "image/png";
        }
        throw new InvalidDocumentException("Upload a PDF, JPEG or PNG file.");
    }

    private static boolean startsWith(byte[] content, byte[] magic) {
        return content.length >= magic.length && Arrays.equals(content, 0, magic.length, magic, 0, magic.length);
    }

    private static String extension(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            default -> ".png";
        };
    }

    /** The file name as shown to staff: no path, no control characters, at most 200 characters. */
    private static String cleanName(String fileName) {
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("\\p{Cntrl}", "")
                .strip();
        if (name.isEmpty()) {
            name = "document";
        }
        return name.length() > 200 ? name.substring(0, 200) : name;
    }
}
