package com.ledgerline.wallets;

import com.ledgerline.DomainException;
import com.ledgerline.ledger.CurrencyCode;
import org.jspecify.annotations.Nullable;

/** A user holds at most one wallet per currency. */
class WalletAlreadyOpenException extends DomainException {

    WalletAlreadyOpenException(CurrencyCode currency, @Nullable Throwable cause) {
        super(Kind.CONFLICT, "Wallet already open", "You already have a " + currency + " wallet.", cause);
    }
}
