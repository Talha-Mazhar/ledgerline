package com.ledgerline.wallets;

import com.ledgerline.ledger.StatementLine;

/**
 * The contract's {@code ActivityKind}: what moved the money, seen from one wallet. A ledger entry
 * kind becomes two activity kinds when the money can go either way.
 */
public enum ActivityKind {
    DEPOSIT,
    WITHDRAWAL,
    EXCHANGE_OUT,
    EXCHANGE_IN,
    TRANSFER_OUT,
    TRANSFER_IN,
    /** Money set aside while the payment waits for approval. */
    HELD,
    /** Held money returned because the payment was not approved. */
    RELEASED,
    REFUND_OUT,
    REFUND_IN;

    static ActivityKind of(StatementLine line) {
        boolean in = line.amount().isPositive();
        return switch (line.kind()) {
            case DEPOSIT -> DEPOSIT;
            case WITHDRAWAL -> WITHDRAWAL;
            case EXCHANGE -> in ? EXCHANGE_IN : EXCHANGE_OUT;
            case TRANSFER -> in ? TRANSFER_IN : TRANSFER_OUT;
            case HOLD -> HELD;
            case RELEASE -> RELEASED;
            case REFUND -> in ? REFUND_IN : REFUND_OUT;
        };
    }
}
