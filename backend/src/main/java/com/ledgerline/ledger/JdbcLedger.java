package com.ledgerline.ledger;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ledger, written in plain SQL with {@link JdbcClient}.
 *
 * <p>Why SQL and not JPA here: the ledger is append-only rows, row locks ({@code FOR UPDATE}) and
 * {@code INSERT ... ON CONFLICT}, which SQL says directly. JPA shines for objects that change over
 * time, like wallets; it adds nothing to rows that never change. Both share one transaction.
 *
 * <p>How a post stays correct when two requests arrive at once:
 *
 * <ol>
 *   <li>The entry row is inserted first with {@code ON CONFLICT DO NOTHING} on the idempotency key.
 *       A concurrent retry with the same key waits for this transaction, then finds the entry and
 *       replays it, so the money moves once.
 *   <li>Every account the entry touches is locked with {@code SELECT ... FOR UPDATE}, always in id
 *       order. Two payments from the same wallet now run one after the other, so both can't spend
 *       the same money; the fixed order means two payments can't wait on each other forever
 *       (a deadlock).
 *   <li>Balances are read while the locks are held, each leg is checked (user accounts can't go
 *       below zero) and the postings are written with their running balance.
 * </ol>
 */
@Service
class JdbcLedger implements Ledger {

    private static final String USER = "USER";

    private final JdbcClient jdbc;
    private final Clock clock;

    JdbcLedger(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void openUserAccount(UUID accountId, UUID ownerId, CurrencyCode currency) {
        jdbc.sql("""
                        INSERT INTO ledger_accounts (id, kind, owner_id, currency, created_at)
                        VALUES (:id, :kind, :owner, :currency, :now)
                        """)
                .param("id", accountId)
                .param("kind", USER)
                .param("owner", ownerId)
                .param("currency", currency.name())
                .param("now", now())
                .update();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID systemAccount(SystemAccount account, CurrencyCode currency) {
        return jdbc.sql("""
                        SELECT id FROM ledger_accounts
                        WHERE kind = :kind AND currency = :currency AND owner_id IS NULL
                        """)
                .param("kind", account.name())
                .param("currency", currency.name())
                .query((rs, row) -> uuid(rs, "id"))
                .single();
    }

    @Override
    @Transactional(readOnly = true)
    public Money balanceOf(UUID accountId) {
        Money balance = balancesOf(List.of(accountId)).get(accountId);
        if (balance == null) {
            throw new IllegalArgumentException("No ledger account " + accountId);
        }
        return balance;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Money> balancesOf(Collection<UUID> accountIds) {
        Map<UUID, Money> balances = new HashMap<>();
        if (accountIds.isEmpty()) {
            return balances;
        }
        jdbc.sql("""
                        SELECT a.id, a.currency,
                               COALESCE((SELECT p.balance_after FROM postings p
                                         WHERE p.account_id = a.id
                                         ORDER BY p.seq DESC LIMIT 1), 0) AS balance
                        FROM ledger_accounts a
                        WHERE a.id IN (:ids)
                        """).param("ids", accountIds).query(rs -> {
            balances.put(uuid(rs, "id"), new Money(rs.getBigDecimal("balance"), currency(rs)));
        });
        return balances;
    }

    @Override
    @Transactional
    public PostedEntry post(JournalEntry entry) {
        UUID entryId = UUID.randomUUID();
        OffsetDateTime postedAt = now();

        int inserted = jdbc.sql("""
                        INSERT INTO journal_entries
                            (id, kind, initiated_by, idempotency_key, request_fingerprint, posted_at)
                        VALUES (:id, :kind, :initiatedBy, :key, :fingerprint, :postedAt)
                        ON CONFLICT (initiated_by, idempotency_key) DO NOTHING
                        """)
                .param("id", entryId)
                .param("kind", entry.kind().name())
                .param("initiatedBy", entry.initiatedBy())
                .param("key", entry.idempotencyKey())
                .param("fingerprint", entry.fingerprint())
                .param("postedAt", postedAt)
                .update();
        if (inserted == 0) {
            return replay(entry);
        }

        Map<UUID, LockedAccount> accounts = lock(entry.accountIds());
        List<StatementLine> lines = new ArrayList<>();
        for (JournalEntry.Leg leg : entry.legs()) {
            LockedAccount account = accounts.get(leg.accountId());
            if (account == null) {
                throw new IllegalArgumentException("No ledger account " + leg.accountId());
            }
            if (account.currency() != leg.amount().currency()) {
                throw new IllegalArgumentException("Account " + account.id() + " holds " + account.currency() + ", not "
                        + leg.amount().currency());
            }
            BigDecimal after = account.balance().add(leg.amount().amount());
            if (account.isUserAccount() && after.signum() < 0) {
                throw new InsufficientFundsException(new Money(account.balance(), account.currency()));
            }
            accounts.put(account.id(), account.withBalance(after));

            UUID postingId = UUID.randomUUID();
            jdbc.sql("""
                            INSERT INTO postings (id, entry_id, account_id, amount, currency, balance_after, memo)
                            VALUES (:id, :entryId, :accountId, :amount, :currency, :balanceAfter, :memo)
                            """)
                    .param("id", postingId)
                    .param("entryId", entryId)
                    .param("accountId", account.id())
                    .param("amount", leg.amount().amount())
                    .param("currency", account.currency().name())
                    .param("balanceAfter", after)
                    .param("memo", leg.memo())
                    .update();
            lines.add(new StatementLine(
                    postingId,
                    entryId,
                    entry.kind(),
                    account.id(),
                    leg.amount(),
                    new Money(after, account.currency()),
                    leg.memo(),
                    postedAt.toInstant()));
        }
        return new PostedEntry(entryId, false, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementLine> statement(Collection<UUID> accountIds, int limit) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                        SELECT p.id, p.entry_id, e.kind, p.account_id, p.amount, p.currency,
                               p.balance_after, p.memo, e.posted_at
                        FROM postings p
                        JOIN journal_entries e ON e.id = p.entry_id
                        WHERE p.account_id IN (:ids)
                        ORDER BY p.seq DESC
                        LIMIT :limit
                        """)
                .param("ids", accountIds)
                .param("limit", limit)
                .query(JdbcLedger::statementLine)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementLine> linesBetween(UUID accountId, Instant from, Instant to) {
        return linesBetween(List.of(accountId), from, to, Integer.MAX_VALUE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementLine> linesBetween(Collection<UUID> accountIds, Instant from, Instant to, int limit) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                        SELECT p.id, p.entry_id, e.kind, p.account_id, p.amount, p.currency,
                               p.balance_after, p.memo, e.posted_at
                        FROM postings p
                        JOIN journal_entries e ON e.id = p.entry_id
                        WHERE p.account_id IN (:ids) AND e.posted_at >= :from AND e.posted_at < :to
                        ORDER BY p.seq
                        LIMIT :limit
                        """)
                .param("ids", accountIds)
                .param("from", OffsetDateTime.ofInstant(from, ZoneOffset.UTC))
                .param("to", OffsetDateTime.ofInstant(to, ZoneOffset.UTC))
                .param("limit", limit)
                .query(JdbcLedger::statementLine)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Money balanceBefore(UUID accountId, Instant moment) {
        record Account(CurrencyCode currency, BigDecimal balance) {}
        Account account = jdbc.sql("""
                        SELECT a.currency,
                               COALESCE((SELECT p.balance_after FROM postings p
                                         JOIN journal_entries e ON e.id = p.entry_id
                                         WHERE p.account_id = a.id AND e.posted_at < :moment
                                         ORDER BY p.seq DESC LIMIT 1), 0) AS balance
                        FROM ledger_accounts a WHERE a.id = :id
                        """)
                .param("id", accountId)
                .param("moment", OffsetDateTime.ofInstant(moment, ZoneOffset.UTC))
                .query((rs, row) -> new Account(currency(rs), rs.getBigDecimal("balance")))
                .single();
        return new Money(account.balance(), account.currency());
    }

    @Override
    @Transactional(readOnly = true)
    public PostedEntry entry(UUID entryId) {
        List<StatementLine> lines = linesOf(entryId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("No journal entry " + entryId);
        }
        return new PostedEntry(entryId, true, lines);
    }

    private List<StatementLine> linesOf(UUID entryId) {
        return jdbc.sql("""
                        SELECT p.id, p.entry_id, e.kind, p.account_id, p.amount, p.currency,
                               p.balance_after, p.memo, e.posted_at
                        FROM postings p
                        JOIN journal_entries e ON e.id = p.entry_id
                        WHERE p.entry_id = :entryId
                        ORDER BY p.seq
                        """)
                .param("entryId", entryId)
                .query(JdbcLedger::statementLine)
                .list();
    }

    /** A retry: same key. Same request → the first result; different request → refused. */
    private PostedEntry replay(JournalEntry entry) {
        record Existing(UUID id, String fingerprint) {}
        Existing existing = jdbc.sql("""
                        SELECT id, request_fingerprint FROM journal_entries
                        WHERE initiated_by = :initiatedBy AND idempotency_key = :key
                        """)
                .param("initiatedBy", entry.initiatedBy())
                .param("key", entry.idempotencyKey())
                .query((rs, row) -> new Existing(uuid(rs, "id"), rs.getString("request_fingerprint")))
                .single();
        if (!existing.fingerprint().equals(entry.fingerprint())) {
            throw new IdempotencyKeyReusedException();
        }
        return new PostedEntry(existing.id(), true, linesOf(existing.id()));
    }

    /** Locks the accounts in id order and reads their balances while the locks are held. */
    private Map<UUID, LockedAccount> lock(Collection<UUID> accountIds) {
        Map<UUID, LockedAccount> accounts = new HashMap<>();
        jdbc.sql("""
                        SELECT id, kind, currency FROM ledger_accounts
                        WHERE id IN (:ids)
                        ORDER BY id
                        FOR UPDATE
                        """).param("ids", accountIds).query(rs -> {
            UUID id = uuid(rs, "id");
            accounts.put(id, new LockedAccount(id, USER.equals(rs.getString("kind")), currency(rs), BigDecimal.ZERO));
        });
        balancesOf(accounts.keySet()).forEach((id, balance) -> {
            LockedAccount account = accounts.get(id);
            if (account != null) {
                accounts.put(id, account.withBalance(balance.amount()));
            }
        });
        return accounts;
    }

    private record LockedAccount(UUID id, boolean isUserAccount, CurrencyCode currency, BigDecimal balance) {

        LockedAccount withBalance(BigDecimal newBalance) {
            return new LockedAccount(id, isUserAccount, currency, newBalance);
        }
    }

    private static StatementLine statementLine(ResultSet rs, int row) throws SQLException {
        CurrencyCode currency = currency(rs);
        return new StatementLine(
                uuid(rs, "id"),
                uuid(rs, "entry_id"),
                EntryKind.valueOf(rs.getString("kind")),
                uuid(rs, "account_id"),
                new Money(rs.getBigDecimal("amount"), currency),
                new Money(rs.getBigDecimal("balance_after"), currency),
                rs.getString("memo"),
                rs.getObject("posted_at", OffsetDateTime.class).toInstant());
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static CurrencyCode currency(ResultSet rs) throws SQLException {
        return CurrencyCode.valueOf(rs.getString("currency"));
    }

    /** The current time in UTC, cut to microseconds like PostgreSQL stores it. */
    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
