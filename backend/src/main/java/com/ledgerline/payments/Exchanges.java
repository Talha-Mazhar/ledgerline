package com.ledgerline.payments;

import com.ledgerline.fx.ExchangeRates;
import com.ledgerline.fx.Rate;
import com.ledgerline.ledger.EntryKind;
import com.ledgerline.ledger.JournalEntry;
import com.ledgerline.ledger.JournalEntry.Leg;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.PostedEntry;
import com.ledgerline.ledger.StatementLine;
import com.ledgerline.ledger.SystemAccount;
import com.ledgerline.wallets.OwnedWallet;
import com.ledgerline.wallets.WalletDirectory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changing money between two of the user's wallets.
 *
 * <p>Selling 100 EUR for 2,502.42 CZK is one journal entry with four legs, balanced in each currency:
 *
 * <pre>
 *   EUR wallet   -100.00 EUR      FX EUR account   +100.00 EUR
 *   FX CZK account -2502.42 CZK   CZK wallet      +2502.42 CZK
 * </pre>
 *
 * Ledgerline's FX accounts are the counterparty: they buy the euros and sell the koruna.
 */
@Service
class Exchanges {

    /** A preview, or the result of an exchange: what was sold, what was bought, at which rate. */
    record Quote(Money sell, Money buy, Rate rate) {}

    /** A completed exchange: the two wallets' new ledger lines. */
    record Completed(UUID entryId, StatementLine sell, StatementLine buy, Rate rate) {}

    private final WalletDirectory wallets;
    private final ExchangeRates rates;
    private final Ledger ledger;
    private final PaymentBook book;

    Exchanges(WalletDirectory wallets, ExchangeRates rates, Ledger ledger, PaymentBook book) {
        this.wallets = wallets;
        this.rates = rates;
        this.ledger = ledger;
        this.book = book;
    }

    @Transactional(readOnly = true)
    public Quote quote(UUID userId, UUID fromWalletId, UUID toWalletId, String typedAmount) {
        return priced(
                wallets.requireOwned(userId, fromWalletId), wallets.requireOwned(userId, toWalletId), typedAmount);
    }

    @Transactional
    public Completed exchange(
            UUID userId, UUID fromWalletId, UUID toWalletId, String typedAmount, UUID idempotencyKey) {
        OwnedWallet from = wallets.requireOwned(userId, fromWalletId);
        OwnedWallet to = wallets.requireOwned(userId, toWalletId);
        Money sell = Amounts.positive(typedAmount, from.currency());
        String fingerprint = "EXCHANGE|" + from.id() + "|" + to.id() + "|" + sell.toPlainString();
        Optional<Payment> earlier = book.earlier(userId, idempotencyKey, fingerprint);
        if (earlier.isPresent()) {
            PostedEntry entry = ledger.entry(earlier.get().entryId());
            return completed(entry, from, to);
        }

        Quote quote = priced(from, to, typedAmount);
        UUID fxSells = ledger.systemAccount(SystemAccount.FX, from.currency());
        UUID fxBuys = ledger.systemAccount(SystemAccount.FX, to.currency());
        PostedEntry posted = ledger.post(new JournalEntry(
                EntryKind.EXCHANGE,
                userId,
                idempotencyKey.toString(),
                fingerprint,
                List.of(
                        new Leg(from.id(), quote.sell().negate(), "Exchanged to " + to.currency()),
                        new Leg(fxSells, quote.sell(), "FX bought " + from.currency()),
                        new Leg(fxBuys, quote.buy().negate(), "FX sold " + to.currency()),
                        new Leg(to.id(), quote.buy(), "Exchanged from " + from.currency()))));
        if (!posted.replayed()) {
            Payment payment = book.save(Payment.request(
                            PaymentKind.EXCHANGE,
                            userId,
                            idempotencyKey.toString(),
                            fingerprint,
                            from.id(),
                            to.id(),
                            null,
                            quote.sell(),
                            book.now())
                    .recorded(posted.entryId()));
            book.publish(book.completed(payment));
        }
        return new Completed(posted.entryId(), posted.lineFor(from.id()), posted.lineFor(to.id()), quote.rate());
    }

    /** An earlier exchange, answered again: the rate is the one it was made at. */
    private Completed completed(PostedEntry entry, OwnedWallet from, OwnedWallet to) {
        StatementLine sold = entry.lineFor(from.id());
        StatementLine bought = entry.lineFor(to.id());
        BigDecimal rate =
                bought.amount().amount().divide(sold.amount().amount().negate(), Rate.SCALE, RoundingMode.HALF_EVEN);
        Rate applied = new Rate(from.currency(), to.currency(), rate, rate, sold.postedAt());
        return new Completed(entry.entryId(), sold, bought, applied);
    }

    private Quote priced(OwnedWallet from, OwnedWallet to, String typedAmount) {
        if (from.currency() == to.currency()) {
            throw new InvalidExchangeException("Choose two wallets in different currencies.");
        }
        Money sell = Amounts.positive(typedAmount, from.currency());
        Rate rate = rates.rate(from.currency(), to.currency());
        Money buy = rate.convert(sell);
        if (!buy.isPositive()) {
            throw new InvalidAmountException("That amount is too small to exchange.");
        }
        return new Quote(sell, buy, rate);
    }
}
