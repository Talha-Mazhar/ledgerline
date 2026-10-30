package com.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTests {

    @Test
    void zeroHasTheCurrencysDecimalPlaces() {
        assertThat(Money.zero(CurrencyCode.EUR).toPlainString()).isEqualTo("0.00");
    }

    @Test
    void padsAmountsToTheCurrencysDecimalPlaces() {
        assertThat(new Money(new BigDecimal("12.5"), CurrencyCode.CZK).toPlainString())
                .isEqualTo("12.50");
    }

    @Test
    void refusesToRoundAwayFractionsOfACent() {
        assertThatExceptionOfType(ArithmeticException.class)
                .isThrownBy(() -> new Money(new BigDecimal("0.001"), CurrencyCode.USD));
    }

    @Test
    void amountsWithTheSameValueAreEqual() {
        assertThat(new Money(new BigDecimal("5"), CurrencyCode.GBP))
                .isEqualTo(new Money(new BigDecimal("5.00"), CurrencyCode.GBP));
    }
}
