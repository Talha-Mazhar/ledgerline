package com.ledgerline.risk;

import com.ledgerline.identity.CurrentUser;
import com.ledgerline.onboarding.CustomerTiers;
import com.ledgerline.onboarding.Tier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /limits}: the customer's tier and what they may send without approval. */
@RestController
class LimitsController {

    record LimitsResponse(Tier tier, String singlePayment, String daily, String currency) {}

    private final PaymentScreening screening;
    private final CustomerTiers tiers;
    private final CurrentUser currentUser;

    LimitsController(PaymentScreening screening, CustomerTiers tiers, CurrentUser currentUser) {
        this.screening = screening;
        this.tiers = tiers;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/limits")
    LimitsResponse getLimits() {
        RiskProperties.TierLimits limits = screening.limitsFor(currentUser.id());
        return new LimitsResponse(
                tiers.tierOf(currentUser.id()),
                limits.singlePayment().setScale(2).toPlainString(),
                limits.daily().setScale(2).toPlainString(),
                "EUR");
    }
}
