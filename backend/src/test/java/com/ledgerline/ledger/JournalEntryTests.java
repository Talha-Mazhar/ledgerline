package com.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.ledgerline.ledger.JournalEntry.Leg;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JournalEntryTests {

    private static final UUID USER = UUID.randomUUID();

    @Test
    void acceptsAnEntryThatBalancesInEachCurrency() {
        JournalEntry exchange = entry(List.of(
                leg("-100", CurrencyCode.EUR),
                leg("100", CurrencyCode.EUR),
                leg("-2500", CurrencyCode.CZK),
                leg("2500", CurrencyCode.CZK)));

        assertThat(exchange.legs()).hasSize(4);
    }

    @Test
    void refusesAnEntryThatDoesNotBalance() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> entry(List.of(leg("-100", CurrencyCode.EUR), leg("99.99", CurrencyCode.EUR))))
                .withMessageContaining("does not balance in EUR");
    }

    @Test
    void refusesAnEntryThatBalancesOnlyAcrossCurrencies() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> entry(List.of(leg("-100", CurrencyCode.EUR), leg("100", CurrencyCode.USD))));
    }

    @Test
    void refusesASingleLeg() {
        assertThatIllegalArgumentException().isThrownBy(() -> entry(List.of(leg("100", CurrencyCode.EUR))));
    }

    @Test
    void refusesALegOfZero() {
        assertThatIllegalArgumentException().isThrownBy(() -> leg("0", CurrencyCode.EUR));
    }

    private static JournalEntry entry(List<Leg> legs) {
        return new JournalEntry(EntryKind.DEPOSIT, USER, "key", "fingerprint", legs);
    }

    private static Leg leg(String amount, CurrencyCode currency) {
        return new Leg(UUID.randomUUID(), new Money(new BigDecimal(amount), currency), "memo");
    }
}
