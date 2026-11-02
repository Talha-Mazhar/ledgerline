package com.ledgerline.wallets;

import com.ledgerline.ledger.CurrencyCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The wallets module's public API: how other modules (payments) find wallets. They never see the
 * entity or the repository.
 */
public interface WalletDirectory {

    /**
     * The user's wallet with this id.
     *
     * @throws com.ledgerline.DomainException (not found) if the user has no such wallet
     */
    OwnedWallet requireOwned(UUID ownerId, UUID walletId);

    /** Every wallet, e.g. for the monthly statement run. */
    List<OwnedWallet> all();

    /** The user's wallet in a currency, if they have one. */
    Optional<OwnedWallet> findByOwnerAndCurrency(UUID ownerId, CurrencyCode currency);
}
