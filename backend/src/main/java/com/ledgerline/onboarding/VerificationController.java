package com.ledgerline.onboarding;

import com.ledgerline.identity.CurrentUser;
import java.io.IOException;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** A customer's own verification: see where it stands, upload a document. */
@RestController
@RequestMapping("/api/v1/verification")
class VerificationController {

    /** The contract's {@code Verification}. */
    record VerificationResponse(
            VerificationStatus status,
            Tier tier,
            @Nullable String documentName,
            @Nullable Instant submittedAt,
            @Nullable Instant reviewedAt,
            @Nullable String rejectionReason) {

        static VerificationResponse notStarted() {
            return new VerificationResponse(VerificationStatus.NOT_STARTED, Tier.BASIC, null, null, null, null);
        }

        static VerificationResponse from(Verification verification) {
            return new VerificationResponse(
                    verification.status(),
                    verification.status() == VerificationStatus.VERIFIED ? Tier.VERIFIED : Tier.BASIC,
                    verification.documentName(),
                    verification.submittedAt(),
                    verification.reviewedAt(),
                    verification.rejectionReason());
        }
    }

    private final Verifications verifications;
    private final CurrentUser currentUser;

    VerificationController(Verifications verifications, CurrentUser currentUser) {
        this.verifications = verifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    VerificationResponse getVerification() {
        return verifications
                .of(currentUser.id())
                .map(VerificationResponse::from)
                .orElseGet(VerificationResponse::notStarted);
    }

    @PostMapping(consumes = "multipart/form-data")
    ResponseEntity<VerificationResponse> submitVerification(@RequestParam("document") MultipartFile document)
            throws IOException {
        String name = document.getOriginalFilename();
        Verification saved = verifications.submit(currentUser.id(), name == null ? "document" : name, document.getBytes());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(VerificationResponse.from(saved));
    }
}
