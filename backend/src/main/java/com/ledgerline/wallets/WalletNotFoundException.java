package com.ledgerline.wallets;

import java.util.UUID;

/** The user has no wallet with this id: either it doesn't exist or it belongs to someone else. */
class WalletNotFoundException extends RuntimeException {

    WalletNotFoundException(UUID walletId) {
        super("You have no wallet with id " + walletId + ".");
    }
}
