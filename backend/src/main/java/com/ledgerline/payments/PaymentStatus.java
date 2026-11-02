package com.ledgerline.payments;

/**
 * Where a payment stands. The allowed moves are the state machine:
 *
 * <pre>
 *   COMPLETED ──refund──▶ REFUNDED
 *   HELD ──first approval──▶ AWAITING_SECOND_APPROVAL ──second approval──▶ COMPLETED
 *   HELD | AWAITING_SECOND_APPROVAL ──reject──▶ REJECTED
 * </pre>
 *
 * Anything else, such as approving a completed payment or refunding a rejected one, is refused.
 */
public enum PaymentStatus {
    COMPLETED,
    HELD,
    AWAITING_SECOND_APPROVAL,
    REJECTED,
    REFUNDED;

    public boolean canMoveTo(PaymentStatus next) {
        return switch (this) {
            case HELD -> next == AWAITING_SECOND_APPROVAL || next == REJECTED;
            case AWAITING_SECOND_APPROVAL -> next == COMPLETED || next == REJECTED;
            case COMPLETED -> next == REFUNDED;
            case REJECTED, REFUNDED -> false;
        };
    }

    /** Waiting for staff: shown in the back office's approval queue. */
    public boolean waitingForApproval() {
        return this == HELD || this == AWAITING_SECOND_APPROVAL;
    }
}
