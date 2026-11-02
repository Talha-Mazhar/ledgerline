package com.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.ledger.JournalEntry.Leg;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The ledger's invariants under load: 100 transfers between 10 accounts, sent from 16 threads at
 * once, many of them racing for the same accounts.
 *
 * <ul>
 *   <li>Every journal entry sums to zero, so all postings in a currency sum to zero.
 *   <li>No account goes below zero, even when transfers race for the same money.
 *   <li>Money is only moved, never created: the 10 accounts hold exactly what they were given.
 *   <li>Each account's balance equals the sum of its postings, and its last {@code balance_after}.
 * </ul>
 *
 * <p>Not {@code @Transactional}: every transfer commits on its own thread like a real request.
 * The accounts belong to random users, so the committed rows don't touch other tests' data.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LedgerInvariantsTests {

    private static final int ACCOUNTS = 10;
    private static final int TRANSFERS = 100;
    private static final BigDecimal START = new BigDecimal("1000.00");

    private final Ledger ledger;
    private final JdbcClient jdbc;

    LedgerInvariantsTests(@Autowired Ledger ledger, @Autowired JdbcClient jdbc) {
        this.ledger = ledger;
        this.jdbc = jdbc;
    }

    @Test
    void oneHundredParallelTransfersKeepTheLedgerBalanced() throws Exception {
        UUID funding = ledger.systemAccount(SystemAccount.FUNDING, CurrencyCode.EUR);
        List<UUID> accounts = new ArrayList<>();
        for (int i = 0; i < ACCOUNTS; i++) {
            UUID owner = UUID.randomUUID();
            UUID account = UUID.randomUUID();
            ledger.openUserAccount(account, owner, CurrencyCode.EUR);
            ledger.post(transfer(EntryKind.DEPOSIT, owner, funding, account, START));
            accounts.add(account);
        }

        RandomGenerator random = RandomGenerator.of("L64X128MixRandom");
        List<Callable<Boolean>> transfers = new ArrayList<>();
        for (int i = 0; i < TRANSFERS; i++) {
            UUID from = accounts.get(random.nextInt(ACCOUNTS));
            UUID to = accounts.get((accounts.indexOf(from) + 1 + random.nextInt(ACCOUNTS - 1)) % ACCOUNTS);
            BigDecimal amount = BigDecimal.valueOf(random.nextInt(1, 40_000), 2);
            transfers.add(() -> {
                try {
                    ledger.post(transfer(EntryKind.TRANSFER, UUID.randomUUID(), from, to, amount));
                    return true;
                } catch (InsufficientFundsException refused) {
                    return false;
                }
            });
        }
        List<Future<Boolean>> results;
        try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
            results = pool.invokeAll(transfers);
        }
        int completed = 0;
        for (Future<Boolean> result : results) {
            completed += result.get() ? 1 : 0;
        }
        assertThat(completed).as("most transfers fit the balances").isGreaterThan(TRANSFERS / 2);

        Map<UUID, Money> balances = ledger.balancesOf(accounts);
        BigDecimal total = BigDecimal.ZERO;
        for (UUID account : accounts) {
            BigDecimal balance = Objects.requireNonNull(balances.get(account)).amount();
            assertThat(balance).as("balance of %s", account).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(balance).as("balance equals its postings").isEqualByComparingTo(sumOfPostings(account));
            total = total.add(balance);
        }
        assertThat(total).as("money was moved, not created").isEqualByComparingTo(START.multiply(BigDecimal.TEN));

        List<@Nullable BigDecimal> perCurrency = jdbc.sql("SELECT SUM(amount) FROM postings GROUP BY currency")
                .query(BigDecimal.class)
                .list();
        assertThat(perCurrency).allSatisfy(sum -> assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO));
    }

    private BigDecimal sumOfPostings(UUID account) {
        return jdbc.sql("SELECT COALESCE(SUM(amount), 0) FROM postings WHERE account_id = :id")
                .param("id", account)
                .query(BigDecimal.class)
                .single();
    }

    private static JournalEntry transfer(EntryKind kind, UUID by, UUID from, UUID to, BigDecimal amount) {
        Money money = new Money(amount, CurrencyCode.EUR);
        String key = UUID.randomUUID().toString();
        return new JournalEntry(
                kind, by, key, kind + "|" + key, List.of(new Leg(from, money.negate(), "out"), new Leg(to, money, "in")));
    }
}
