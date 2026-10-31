package com.ledgerline.wallets;

import com.ledgerline.DomainException;
import java.util.UUID;

/** The user has no wallet with this id: either it doesn't exist or it belongs to someone else. */
class WalletNotFoundException extends DomainException {

    WalletNotFoundException(UUID walletId) {
        super(Kind.NOT_FOUND, "Wallet not found", "You have no wallet with id " + walletId + ".", null);
    }
}
