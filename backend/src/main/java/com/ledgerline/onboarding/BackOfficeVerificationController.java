package com.ledgerline.onboarding;

import com.ledgerline.documents.DocumentStore.StoredDocument;
import com.ledgerline.identity.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The review queue for identity documents. Staff only (every {@code /back-office} path is). */
@RestController
@RequestMapping("/api/v1/back-office/verifications")
class BackOfficeVerificationController {

    record ReviewItemResponse(
            UUID userId,
            String email,
            String displayName,
            VerificationStatus status,
            String documentName,
            String contentType,
            Instant submittedAt) {

        static ReviewItemResponse from(Verifications.ReviewItem item) {
            Verification verification = item.verification();
            return new ReviewItemResponse(
                    item.user().id(),
                    item.user().email(),
                    item.user().displayName(),
                    verification.status(),
                    verification.documentName(),
                    verification.contentType(),
                    verification.submittedAt());
        }
    }

    record ReviewQueueResponse(List<ReviewItemResponse> items) {}

    record RejectRequest(String reason) {
        RejectRequest {
            Objects.requireNonNull(reason, "reason is required");
        }
    }

    private final Verifications verifications;
    private final CurrentUser currentUser;

    BackOfficeVerificationController(Verifications verifications, CurrentUser currentUser) {
        this.verifications = verifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    ReviewQueueResponse listVerificationsToReview() {
        return new ReviewQueueResponse(verifications.waitingForReview().stream()
                .map(ReviewItemResponse::from)
                .toList());
    }

    /** The uploaded file, shown inline in the browser and never cached. */
    @GetMapping("/{userId}/document")
    ResponseEntity<byte[]> getVerificationDocument(@PathVariable UUID userId) {
        StoredDocument document = verifications.document(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(document.content());
    }

    @PostMapping("/{userId}/approve")
    VerificationController.VerificationResponse approveVerification(@PathVariable UUID userId) {
        return VerificationController.VerificationResponse.from(verifications.approve(currentUser.id(), userId));
    }

    @PostMapping("/{userId}/reject")
    VerificationController.VerificationResponse rejectVerification(
            @PathVariable UUID userId, @RequestBody RejectRequest body) {
        return VerificationController.VerificationResponse.from(
                verifications.reject(currentUser.id(), userId, body.reason()));
    }
}
