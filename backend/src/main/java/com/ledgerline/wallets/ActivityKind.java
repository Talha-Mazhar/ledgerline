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
    TRANSFER_IN;

    static ActivityKind of(StatementLine line) {
        boolean in = line.amount().isPositive();
        return switch (line.kind()) {
            case DEPOSIT -> DEPOSIT;
            case WITHDRAWAL -> WITHDRAWAL;
            case EXCHANGE -> in ? EXCHANGE_IN : EXCHANGE_OUT;
            case TRANSFER -> in ? TRANSFER_IN : TRANSFER_OUT;
        };
    }
}
