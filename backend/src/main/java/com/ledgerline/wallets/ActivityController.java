package com.ledgerline.wallets;

import com.ledgerline.identity.CurrentUser;
import com.ledgerline.ledger.StatementLine;
import com.ledgerline.wallets.WalletResponses.ActivityListResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Activity: the ledger lines of the user's wallets, newest first. {@code limit} is checked by bean
 * validation ({@code @Min}, {@code @Max}); Spring answers an out-of-range value with 400.
 */
@RestController
@RequestMapping("/api/v1")
class ActivityController {

    private final WalletService wallets;
    private final CurrentUser currentUser;

    ActivityController(WalletService wallets, CurrentUser currentUser) {
        this.wallets = wallets;
        this.currentUser = currentUser;
    }

    @GetMapping("/wallets/{walletId}/activity")
    public ActivityListResponse listWalletActivity(
            @PathVariable UUID walletId, @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return toResponse(wallets.activityOf(currentUser.id(), walletId, limit));
    }

    @GetMapping("/activity")
    public ActivityListResponse listActivity(@RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return toResponse(wallets.activityOf(currentUser.id(), limit));
    }

    private static ActivityListResponse toResponse(List<StatementLine> lines) {
        return new ActivityListResponse(
                lines.stream().map(ActivityItemResponse::from).toList());
    }
}
