package com.ledgerline.risk;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The rules' numbers, from {@code ledgerline.risk.*} in application.yaml. Amounts are in euros;
 * payments in other currencies are compared at the mid rate.
 *
 * @param basic limits for customers who haven't verified their identity
 * @param verified limits for verified customers
 * @param velocityWindow how far back the velocity rule looks
 * @param velocityMaxPayments how many outgoing payments in that window are normal
 * @param newRecipientThreshold a first payment to someone above this is held
 */
@ConfigurationProperties("ledgerline.risk")
public record RiskProperties(
        TierLimits basic,
        TierLimits verified,
        Duration velocityWindow,
        int velocityMaxPayments,
        BigDecimal newRecipientThreshold) {

    /**
     * @param singlePayment the most one outgoing payment may be
     * @param daily the most all outgoing payments of the last 24 hours may add up to
     */
    public record TierLimits(BigDecimal singlePayment, BigDecimal daily) {}
}
