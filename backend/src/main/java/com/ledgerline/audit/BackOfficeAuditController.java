package com.ledgerline.audit;

import com.ledgerline.identity.UserDirectory;
import com.ledgerline.identity.UserSummary;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /back-office/audit}: the latest audit entries, with the staff member's name. */
@RestController
@Validated
class BackOfficeAuditController {

    record AuditEntryResponse(
            UUID id,
            @Nullable String actor,
            String action,
            String targetType,
            UUID targetId,
            String details,
            Instant occurredAt) {}

    record AuditListResponse(List<AuditEntryResponse> items) {}

    private final AuditLog log;
    private final UserDirectory users;

    BackOfficeAuditController(AuditLog log, UserDirectory users) {
        this.log = log;
        this.users = users;
    }

    @GetMapping("/api/v1/back-office/audit")
    AuditListResponse listAuditEntries(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return new AuditListResponse(log.latest(limit).stream()
                .map(entry -> new AuditEntryResponse(
                        entry.id(),
                        actorName(entry.actorId()),
                        entry.action(),
                        entry.targetType(),
                        entry.targetId(),
                        entry.details(),
                        entry.occurredAt()))
                .toList());
    }

    private @Nullable String actorName(@Nullable UUID actor) {
        return actor == null ? null : users.find(actor).map(UserSummary::displayName).orElse(null);
    }
}
