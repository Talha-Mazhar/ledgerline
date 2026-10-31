package com.ledgerline.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RateTests {

    private static final Rate EUR_TO_CZK = new Rate(
            CurrencyCode.EUR, CurrencyCode.CZK, new BigDecimal("25.150000"), new BigDecimal("25.024250"), Instant.EPOCH);

    @Test
    void convertsAtTheCustomerRate() {
        Money bought = EUR_TO_CZK.convert(new Money(new BigDecimal("100"), CurrencyCode.EUR));

        assertThat(bought).isEqualTo(new Money(new BigDecimal("2502.42"), CurrencyCode.CZK));
    }

    @Test
    void roundsDownToTheSmallestUnit() {
        // 0.01 EUR x 25.024250 = 0.2502425 CZK, which rounds down to 0.25.
        Money bought = EUR_TO_CZK.convert(new Money(new BigDecimal("0.01"), CurrencyCode.EUR));

        assertThat(bought.toPlainString()).isEqualTo("0.25");
    }

    @Test
    void refusesToSellTheWrongCurrency() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EUR_TO_CZK.convert(new Money(BigDecimal.TEN, CurrencyCode.USD)));
    }
}
