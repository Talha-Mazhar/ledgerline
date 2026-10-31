package com.ledgerline.payments;

import com.ledgerline.identity.CurrentUser;
import com.ledgerline.ledger.StatementLine;
import com.ledgerline.wallets.ActivityItemResponse;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /wallets/{walletId}/deposits} and {@code .../withdrawals}. Both require an
 * {@code Idempotency-Key} header (a UUID the client creates once per movement); without it Spring
 * answers 400. A retry with the same key gets the original line back and moves no money.
 */
@RestController
@RequestMapping("/api/v1/wallets/{walletId}")
class MoneyMovementController {

    /** Request body: the amount as the user typed it, e.g. {@code "250.50"}. */
    record AmountRequest(String amount) {
        AmountRequest {
            Objects.requireNonNull(amount, "amount is required");
        }
    }

    private final MoneyMovements movements;
    private final CurrentUser currentUser;

    MoneyMovementController(MoneyMovements movements, CurrentUser currentUser) {
        this.movements = movements;
        this.currentUser = currentUser;
    }

    @PostMapping("/deposits")
    public ResponseEntity<ActivityItemResponse> depositMoney(
            @PathVariable UUID walletId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody AmountRequest request) {
        return created(movements.deposit(currentUser.id(), walletId, request.amount(), idempotencyKey));
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<ActivityItemResponse> withdrawMoney(
            @PathVariable UUID walletId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody AmountRequest request) {
        return created(movements.withdraw(currentUser.id(), walletId, request.amount(), idempotencyKey));
    }

    private static ResponseEntity<ActivityItemResponse> created(StatementLine line) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ActivityItemResponse.from(line));
    }
}
