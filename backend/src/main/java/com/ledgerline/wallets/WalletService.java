package com.ledgerline.wallets;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.StatementLine;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opening and reading wallets. The controllers handle HTTP; the rules live here. It also implements
 * the module's public {@link WalletDirectory}, so other modules find wallets through it.
 */
@Service
class WalletService implements WalletDirectory {

    private final WalletRepository wallets;
    private final Ledger ledger;
    private final Clock clock;

    WalletService(WalletRepository wallets, Ledger ledger, Clock clock) {
        this.wallets = wallets;
        this.ledger = ledger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Wallet> walletsOf(UUID ownerId) {
        return wallets.findByOwnerIdOrderByOpenedAtAsc(ownerId);
    }

    @Transactional(readOnly = true)
    public Wallet walletOf(UUID ownerId, UUID walletId) {
        return wallets.findByIdAndOwnerId(walletId, ownerId).orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    /**
     * Opens a wallet in {@code currency} with its ledger account, or fails if the user already has
     * one.
     *
     * <p>The check before saving gives a clear error in the normal case. Two requests at the same
     * moment can both pass it, so the database's unique constraint on (owner, currency) is the real
     * guarantee; {@code saveAndFlush} makes that constraint fire here, inside this method, where it
     * becomes the same error. The wallet and its account commit together or not at all.
     */
    @Transactional
    public Wallet open(UUID ownerId, CurrencyCode currency) {
        if (wallets.existsByOwnerIdAndCurrency(ownerId, currency)) {
            throw new WalletAlreadyOpenException(currency, null);
        }
        Wallet wallet;
        try {
            wallet = wallets.saveAndFlush(Wallet.open(ownerId, currency, clock.instant()));
        } catch (DataIntegrityViolationException raced) {
            throw new WalletAlreadyOpenException(currency, raced);
        }
        ledger.openUserAccount(wallet.id(), ownerId, currency);
        return wallet;
    }

    /** A wallet's balance, read from its ledger account. */
    @Transactional(readOnly = true)
    public Money balanceOf(Wallet wallet) {
        return ledger.balanceOf(wallet.id());
    }

    /** The balances of several wallets in one query, keyed by wallet id. */
    @Transactional(readOnly = true)
    public Map<UUID, Money> balancesOf(List<Wallet> owned) {
        return ledger.balancesOf(owned.stream().map(Wallet::id).toList());
    }

    /** One wallet's latest ledger lines, newest first. */
    @Transactional(readOnly = true)
    public List<StatementLine> activityOf(UUID ownerId, UUID walletId, int limit) {
        Wallet wallet = walletOf(ownerId, walletId);
        return ledger.statement(List.of(wallet.id()), limit);
    }

    /** The latest ledger lines across all the user's wallets, newest first. */
    @Transactional(readOnly = true)
    public List<StatementLine> activityOf(UUID ownerId, int limit) {
        return ledger.statement(walletsOf(ownerId).stream().map(Wallet::id).toList(), limit);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnedWallet requireOwned(UUID ownerId, UUID walletId) {
        return toOwned(walletOf(ownerId, walletId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnedWallet> findByOwnerAndCurrency(UUID ownerId, CurrencyCode currency) {
        return wallets.findByOwnerIdAndCurrency(ownerId, currency).map(WalletService::toOwned);
    }

    private static OwnedWallet toOwned(Wallet wallet) {
        return new OwnedWallet(wallet.id(), wallet.ownerId(), wallet.currency());
    }
}
