package com.ledgerline.payments;

import com.ledgerline.identity.CurrentUser;
import com.ledgerline.wallets.ActivityItemResponse;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /transfers}: send money to another user. Idempotent like every money movement. */
@RestController
@RequestMapping("/api/v1/transfers")
class TransferController {

    record TransferRequest(UUID fromWalletId, String recipientEmail, String amount) {
        TransferRequest {
            Objects.requireNonNull(fromWalletId, "fromWalletId is required");
            Objects.requireNonNull(recipientEmail, "recipientEmail is required");
            Objects.requireNonNull(amount, "amount is required");
        }
    }

    record RecipientResponse(String email, String displayName) {}

    /**
     * @param status COMPLETED when the money arrived, HELD when it waits for staff approval
     */
    record TransferResponse(
            ActivityItemResponse activity, RecipientResponse recipient, UUID paymentId, PaymentStatus status) {}

    private final Transfers transfers;
    private final CurrentUser currentUser;

    TransferController(Transfers transfers, CurrentUser currentUser) {
        this.transfers = transfers;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> sendMoney(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey, @RequestBody TransferRequest request) {
        Transfers.Sent sent = transfers.send(
                currentUser.id(), request.fromWalletId(), request.recipientEmail(), request.amount(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TransferResponse(
                        ActivityItemResponse.from(sent.line()),
                        new RecipientResponse(
                                sent.recipient().email(), sent.recipient().displayName()),
                        sent.payment().id(),
                        sent.payment().status()));
    }
}
