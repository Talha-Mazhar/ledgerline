package com.ledgerline.risk;

import com.ledgerline.ledger.Money;
import java.util.List;
import java.util.UUID;

/**
 * What the risk rules need to know about an outgoing payment. The payments module fills it in,
 * because it owns the payment history.
 *
 * @param amount the payment
 * @param sentLast24Hours earlier outgoing payments of the last 24 hours, in their own currencies
 * @param paymentsInVelocityWindow how many outgoing payments the customer made in the velocity window
 * @param newRecipient true when this is the first transfer to this recipient (false for withdrawals)
 */
public record ScreeningRequest(
        UUID customerId, Money amount, List<Money> sentLast24Hours, int paymentsInVelocityWindow, boolean newRecipient) {

    public ScreeningRequest {
        sentLast24Hours = List.copyOf(sentLast24Hours);
    }
}
