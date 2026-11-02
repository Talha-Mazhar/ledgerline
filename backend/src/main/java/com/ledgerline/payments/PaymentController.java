package com.ledgerline.payments;

import com.ledgerline.identity.CurrentUser;
import com.ledgerline.identity.UserDirectory;
import com.ledgerline.identity.UserSummary;
import com.ledgerline.wallets.MoneyResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /payments}: the customer's payments and where each one stands. */
@RestController
@Validated
class PaymentController {

    enum Direction {
        IN,
        OUT
    }

    /** The contract's {@code Payment}: one payment from the signed-in customer's point of view. */
    record PaymentResponse(
            UUID id,
            PaymentKind kind,
            PaymentStatus status,
            Direction direction,
            String description,
            MoneyResponse amount,
            @Nullable String holdReason,
            @Nullable String rejectionReason,
            Instant createdAt,
            Instant updatedAt) {}

    record PaymentListResponse(List<PaymentResponse> items) {}

    private final PaymentRepository payments;
    private final UserDirectory users;
    private final CurrentUser currentUser;

    PaymentController(PaymentRepository payments, UserDirectory users, CurrentUser currentUser) {
        this.payments = payments;
        this.users = users;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/payments")
    PaymentListResponse listPayments(@RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        UUID me = currentUser.id();
        return new PaymentListResponse(payments.involving(me, PageRequest.of(0, limit)).stream()
                .map(payment -> view(payment, me))
                .toList());
    }

    private PaymentResponse view(Payment payment, UUID me) {
        boolean mine = payment.initiatedBy().equals(me);
        Direction direction = mine && payment.kind() != PaymentKind.DEPOSIT ? Direction.OUT : Direction.IN;
        return new PaymentResponse(
                payment.id(),
                payment.kind(),
                payment.status(),
                direction,
                describe(payment, mine),
                MoneyResponse.from(payment.amount()),
                payment.holdReason(),
                payment.rejectionReason(),
                payment.createdAt(),
                payment.updatedAt());
    }

    private String describe(Payment payment, boolean mine) {
        return switch (payment.kind()) {
            case DEPOSIT -> "Added money";
            case WITHDRAWAL -> "Withdrawal to bank";
            case EXCHANGE -> "Currency exchange";
            case TRANSFER -> mine ? "Sent to " + name(payment.counterpartyId()) : "Received from " + name(payment.initiatedBy());
            case REFUND -> "Refund";
        };
    }

    private String name(@Nullable UUID user) {
        return user == null ? "someone" : users.find(user).map(UserSummary::displayName).orElse("a former user");
    }
}
