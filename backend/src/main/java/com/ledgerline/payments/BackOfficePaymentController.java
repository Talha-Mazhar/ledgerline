package com.ledgerline.payments;

import com.ledgerline.identity.CurrentUser;
import com.ledgerline.identity.UserDirectory;
import com.ledgerline.identity.UserSummary;
import com.ledgerline.wallets.MoneyResponse;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payments in the back office: the approval queue, approve, reject and refund. Staff only (every
 * {@code /back-office} path is).
 */
@RestController
@RequestMapping("/api/v1/back-office/payments")
class BackOfficePaymentController {

    /** Who a payment involves, as staff see them. */
    record Party(UUID id, String email, String displayName) {
        static Party of(UserSummary user) {
            return new Party(user.id(), user.email(), user.displayName());
        }
    }

    /** The contract's {@code StaffPayment}. */
    record StaffPaymentResponse(
            UUID id,
            PaymentKind kind,
            PaymentStatus status,
            MoneyResponse amount,
            Party customer,
            @Nullable Party counterparty,
            @Nullable String holdReason,
            @Nullable String firstApprover,
            @Nullable String secondApprover,
            @Nullable String rejectionReason,
            boolean refundable,
            Instant createdAt,
            Instant updatedAt) {}

    record StaffPaymentListResponse(List<StaffPaymentResponse> items) {}

    record RejectRequest(String reason) {
        RejectRequest {
            Objects.requireNonNull(reason, "reason is required");
        }
    }

    private final PaymentRepository payments;
    private final Approvals approvals;
    private final Refunds refunds;
    private final UserDirectory users;
    private final CurrentUser currentUser;

    BackOfficePaymentController(
            PaymentRepository payments,
            Approvals approvals,
            Refunds refunds,
            UserDirectory users,
            CurrentUser currentUser) {
        this.payments = payments;
        this.approvals = approvals;
        this.refunds = refunds;
        this.users = users;
        this.currentUser = currentUser;
    }

    /**
     * Payments by status. Without {@code status}, the approval queue (held and half-approved),
     * oldest first; with it, the latest 100 in those states, newest first.
     */
    @GetMapping
    StaffPaymentListResponse listStaffPayments(@RequestParam(required = false) @Nullable Set<PaymentStatus> status) {
        List<Payment> found = status == null || status.isEmpty()
                ? payments.findByStatusInOrderByCreatedAt(
                        EnumSet.of(PaymentStatus.HELD, PaymentStatus.AWAITING_SECOND_APPROVAL), PageRequest.of(0, 100))
                : payments.findByStatusInOrderByCreatedAtDesc(status, PageRequest.of(0, 100));
        return new StaffPaymentListResponse(found.stream().map(this::view).toList());
    }

    @PostMapping("/{paymentId}/approve")
    StaffPaymentResponse approvePayment(@PathVariable UUID paymentId) {
        return view(approvals.approve(currentUser.id(), paymentId));
    }

    @PostMapping("/{paymentId}/reject")
    StaffPaymentResponse rejectPayment(@PathVariable UUID paymentId, @RequestBody RejectRequest body) {
        return view(approvals.reject(currentUser.id(), paymentId, body.reason()));
    }

    /** Refunds a completed transfer or top-up; answers with the original payment, now REFUNDED. */
    @PostMapping("/{paymentId}/refund")
    StaffPaymentResponse refundPayment(@PathVariable UUID paymentId) {
        refunds.refund(currentUser.id(), paymentId);
        return view(payments.findById(paymentId).orElseThrow(PaymentNotFoundException::new));
    }

    private StaffPaymentResponse view(Payment payment) {
        UUID counterparty = payment.counterpartyId();
        boolean refundable = payment.status() == PaymentStatus.COMPLETED
                && (payment.kind() == PaymentKind.TRANSFER || payment.kind() == PaymentKind.DEPOSIT);
        return new StaffPaymentResponse(
                payment.id(),
                payment.kind(),
                payment.status(),
                MoneyResponse.from(payment.amount()),
                Party.of(users.require(payment.initiatedBy())),
                counterparty == null ? null : Party.of(users.require(counterparty)),
                payment.holdReason(),
                nameOf(payment.firstApprover()),
                nameOf(payment.secondApprover()),
                payment.rejectionReason(),
                refundable,
                payment.createdAt(),
                payment.updatedAt());
    }

    private @Nullable String nameOf(@Nullable UUID user) {
        return user == null ? null : users.require(user).displayName();
    }
}
