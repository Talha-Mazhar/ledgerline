package com.ledgerline.wallets;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opening and reading wallets. The controller handles HTTP; the rules live here. */
@Service
class WalletService {

    private final WalletRepository wallets;
    private final Clock clock;

    WalletService(WalletRepository wallets, Clock clock) {
        this.wallets = wallets;
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
     * Opens a wallet in {@code currency}, or fails if the user already has one.
     *
     * <p>The check before saving gives a clear error in the normal case. Two requests at the same
     * moment can both pass it, so the database's unique constraint on (owner, currency) is the real
     * guarantee; {@code saveAndFlush} makes that constraint fire here, inside this method, where it
     * becomes the same error.
     */
    @Transactional
    public Wallet open(UUID ownerId, CurrencyCode currency) {
        if (wallets.existsByOwnerIdAndCurrency(ownerId, currency)) {
            throw new WalletAlreadyOpenException(currency, null);
        }
        try {
            return wallets.saveAndFlush(Wallet.open(ownerId, currency, clock.instant()));
        } catch (DataIntegrityViolationException raced) {
            throw new WalletAlreadyOpenException(currency, raced);
        }
    }

    /**
     * A wallet's balance. Wallets open empty and nothing moves money yet, so this is zero; once the
     * ledger records transactions, this asks the ledger for the wallet account's balance instead.
     */
    public Money balanceOf(Wallet wallet) {
        return Money.zero(wallet.currency());
    }
}
