package com.ledgerline.risk;

import java.util.List;

/**
 * Whether a payment goes ahead. A held payment waits for two staff approvals; {@code reasons}
 * says why, in words staff and the customer understand.
 */
public record ScreeningDecision(List<String> reasons) {

    public ScreeningDecision {
        reasons = List.copyOf(reasons);
    }

    public static ScreeningDecision allow() {
        return new ScreeningDecision(List.of());
    }

    public boolean hold() {
        return !reasons.isEmpty();
    }
}
