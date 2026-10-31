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
import java.util.List;
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

    Exchanges(WalletDirectory wallets, ExchangeRates rates, Ledger ledger) {
        this.wallets = wallets;
        this.rates = rates;
        this.ledger = ledger;
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
        Quote quote = priced(from, to, typedAmount);
        UUID fxSells = ledger.systemAccount(SystemAccount.FX, from.currency());
        UUID fxBuys = ledger.systemAccount(SystemAccount.FX, to.currency());

        PostedEntry posted = ledger.post(new JournalEntry(
                EntryKind.EXCHANGE,
                userId,
                idempotencyKey.toString(),
                "EXCHANGE|" + from.id() + "|" + to.id() + "|" + quote.sell().toPlainString(),
                List.of(
                        new Leg(from.id(), quote.sell().negate(), "Exchanged to " + to.currency()),
                        new Leg(fxSells, quote.sell(), "FX bought " + from.currency()),
                        new Leg(fxBuys, quote.buy().negate(), "FX sold " + to.currency()),
                        new Leg(to.id(), quote.buy(), "Exchanged from " + from.currency()))));
        return new Completed(posted.entryId(), posted.lineFor(from.id()), posted.lineFor(to.id()), quote.rate());
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
