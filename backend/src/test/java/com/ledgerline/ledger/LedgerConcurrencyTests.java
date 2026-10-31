package com.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.ledger.JournalEntry.Leg;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Two withdrawals of 80 from an account holding 100, at the same moment. Without the account lock
 * both would read 100, both would pass the check, and the balance would end at -60. With it, one
 * waits for the other, sees 20, and is refused.
 *
 * <p>Not {@code @Transactional}: each withdrawal must commit in its own transaction, on its own
 * thread, like two real requests. The account belongs to a fresh random user, so the committed
 * rows don't affect other tests.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LedgerConcurrencyTests {

    private final Ledger ledger;

    LedgerConcurrencyTests(@Autowired Ledger ledger) {
        this.ledger = ledger;
    }

    @Test
    void twoWithdrawalsAtOnceCannotOverdrawAnAccount() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        ledger.openUserAccount(account, owner, CurrencyCode.EUR);
        UUID funding = ledger.systemAccount(SystemAccount.FUNDING, CurrencyCode.EUR);
        UUID payout = ledger.systemAccount(SystemAccount.PAYOUT, CurrencyCode.EUR);
        ledger.post(entry(EntryKind.DEPOSIT, owner, "deposit", funding, account, "100"));

        Callable<Boolean> withdraw80 = () -> {
            try {
                ledger.post(entry(EntryKind.WITHDRAWAL, owner, UUID.randomUUID().toString(), account, payout, "80"));
                return true;
            } catch (InsufficientFundsException refused) {
                return false;
            }
        };
        List<Future<Boolean>> results;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            results = pool.invokeAll(List.of(withdraw80, withdraw80));
        }

        long succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        assertThat(succeeded).isEqualTo(1);
        assertThat(ledger.balanceOf(account).toPlainString()).isEqualTo("20.00");
    }

    private static JournalEntry entry(EntryKind kind, UUID owner, String key, UUID from, UUID to, String amount) {
        Money money = new Money(new BigDecimal(amount), CurrencyCode.EUR);
        return new JournalEntry(
                kind,
                owner,
                key,
                kind + "|" + key,
                List.of(new Leg(from, money.negate(), "out"), new Leg(to, money, "in")));
    }
}
