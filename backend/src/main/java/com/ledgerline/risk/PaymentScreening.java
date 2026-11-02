package com.ledgerline.risk;

import com.ledgerline.fx.ExchangeRates;
import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import com.ledgerline.onboarding.CustomerTiers;
import com.ledgerline.onboarding.Tier;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The risk module's public API: screens one outgoing payment against the limits and velocity
 * rules. Pure logic over the request and the customer's tier, so it is easy to test exhaustively.
 */
@Service
public class PaymentScreening {

    private final RiskProperties rules;
    private final CustomerTiers tiers;
    private final ExchangeRates rates;

    PaymentScreening(RiskProperties rules, CustomerTiers tiers, ExchangeRates rates) {
        this.rules = rules;
        this.tiers = tiers;
        this.rates = rates;
    }

    public ScreeningDecision screen(ScreeningRequest request) {
        RiskProperties.TierLimits limits = limitsFor(request.customerId());
        BigDecimal amount = inEuros(request.amount());
        BigDecimal earlier = request.sentLast24Hours().stream()
                .map(this::inEuros)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> reasons = new ArrayList<>();
        if (amount.compareTo(limits.singlePayment()) > 0) {
            reasons.add("Above the single-payment limit of " + euros(limits.singlePayment()) + ".");
        }
        if (earlier.add(amount).compareTo(limits.daily()) > 0) {
            reasons.add("Above the 24-hour limit of " + euros(limits.daily()) + ".");
        }
        if (request.paymentsInVelocityWindow() >= rules.velocityMaxPayments()) {
            reasons.add("More than " + rules.velocityMaxPayments() + " payments in "
                    + rules.velocityWindow().toMinutes() + " minutes.");
        }
        if (request.newRecipient() && amount.compareTo(rules.newRecipientThreshold()) > 0) {
            reasons.add("First payment to this recipient is above " + euros(rules.newRecipientThreshold()) + ".");
        }
        return new ScreeningDecision(reasons);
    }

    /** How far back the velocity rule counts payments. */
    public Duration velocityWindow() {
        return rules.velocityWindow();
    }

    /** The limits that apply to this customer now. */
    public RiskProperties.TierLimits limitsFor(UUID customerId) {
        return tiers.tierOf(customerId) == Tier.VERIFIED ? rules.verified() : rules.basic();
    }

    private BigDecimal inEuros(Money money) {
        return rates.valueIn(money, CurrencyCode.EUR).amount();
    }

    /** E.g. {@code €1,000.00}. */
    static String euros(BigDecimal amount) {
        return "€" + String.format(Locale.ROOT, "%,.2f", amount);
    }
}
