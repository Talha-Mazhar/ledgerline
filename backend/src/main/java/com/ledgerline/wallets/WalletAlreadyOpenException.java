package com.ledgerline.wallets;

import com.ledgerline.ledger.CurrencyCode;
import org.jspecify.annotations.Nullable;

/** A user holds at most one wallet per currency. */
class WalletAlreadyOpenException extends RuntimeException {

    WalletAlreadyOpenException(CurrencyCode currency, @Nullable Throwable cause) {
        super("You already have a " + currency + " wallet.", cause);
    }
}
