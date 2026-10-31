package com.ledgerline.payments;

import com.ledgerline.DomainException;

/** A transfer that can't go ahead: no such recipient, no wallet to receive it, or to oneself. */
class TransferRefusedException extends DomainException {

    private TransferRefusedException(Kind kind, String title, String message) {
        super(kind, title, message, null);
    }

    static TransferRefusedException unknownRecipient() {
        return new TransferRefusedException(
                Kind.NOT_FOUND, "Recipient not found", "Nobody on Ledgerline uses this email.");
    }

    static TransferRefusedException toSelf() {
        return new TransferRefusedException(
                Kind.INVALID_REQUEST,
                "Can't send to yourself",
                "To move money between your own wallets, use Exchange.");
    }

    static TransferRefusedException noWallet(String recipientName, String currency) {
        return new TransferRefusedException(
                Kind.RULE_VIOLATION,
                "Recipient can't receive " + currency,
                recipientName + " has no " + currency + " wallet yet.");
    }
}
