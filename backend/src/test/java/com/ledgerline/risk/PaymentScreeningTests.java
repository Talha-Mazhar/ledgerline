package com.ledgerline.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ledgerline.fx.ExchangeRates;
import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import com.ledgerline.onboarding.CustomerTiers;
import com.ledgerline.onboarding.Tier;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The risk rules as plain logic: a fake tier lookup and fixed rates, no Spring, no database. */
class PaymentScreeningTests {

    private static final UUID CUSTOMER = UUID.randomUUID();

    private final CustomerTiers tiers = mock(CustomerTiers.class);
    private final ExchangeRates rates = mock(ExchangeRates.class);
    private final PaymentScreening screening = new PaymentScreening(
            new RiskProperties(
                    new RiskProperties.TierLimits(new BigDecimal("1000"), new BigDecimal("2000")),
                    new RiskProperties.TierLimits(new BigDecimal("10000"), new BigDecimal("25000")),
                    Duration.ofMinutes(10),
                    5,
                    new BigDecimal("500")),
            tiers,
            rates);

    @BeforeEach
    void basicCustomerAndOneToOneRates() {
        when(tiers.tierOf(CUSTOMER)).thenReturn(Tier.BASIC);
        when(rates.valueIn(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(CurrencyCode.EUR)))
                .thenAnswer(call -> {
                    Money money = call.getArgument(0);
                    return new Money(money.amount(), CurrencyCode.EUR);
                });
    }

    @Test
    void anOrdinaryPaymentGoesThrough() {
        assertThat(screening.screen(request("200", List.of(), 0, false)).hold()).isFalse();
    }

    @Test
    void holdsAPaymentAboveTheSingleLimit() {
        assertThat(screening.screen(request("1000.01", List.of(), 0, false)).reasons())
                .containsExactly("Above the single-payment limit of €1,000.00.");
    }

    @Test
    void addsUpTheLast24Hours() {
        assertThat(screening.screen(request("600", List.of(eur("800"), eur("700")), 2, false)).reasons())
                .containsExactly("Above the 24-hour limit of €2,000.00.");
    }

    @Test
    void holdsTheSixthPaymentInTenMinutes() {
        assertThat(screening.screen(request("10", List.of(), 5, false)).reasons())
                .containsExactly("More than 5 payments in 10 minutes.");
    }

    @Test
    void holdsALargeFirstPaymentToSomeoneNew() {
        assertThat(screening.screen(request("600", List.of(), 0, true)).reasons())
                .containsExactly("First payment to this recipient is above €500.00.");
        assertThat(screening.screen(request("600", List.of(), 0, false)).hold()).isFalse();
    }

    @Test
    void verifiedCustomersHaveHigherLimits() {
        when(tiers.tierOf(CUSTOMER)).thenReturn(Tier.VERIFIED);

        assertThat(screening.screen(request("5000", List.of(), 0, false)).hold()).isFalse();
        assertThat(screening.limitsFor(CUSTOMER).daily()).isEqualByComparingTo("25000");
    }

    private static ScreeningRequest request(String amount, List<Money> earlier, int recent, boolean newRecipient) {
        return new ScreeningRequest(CUSTOMER, eur(amount), earlier, recent, newRecipient);
    }

    private static Money eur(String amount) {
        return new Money(new BigDecimal(amount), CurrencyCode.EUR);
    }
}
