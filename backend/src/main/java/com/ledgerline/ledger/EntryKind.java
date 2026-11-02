package com.ledgerline.ledger;

/** What a journal entry records. */
public enum EntryKind {
    /** Money added to a wallet from outside Ledgerline, e.g. a card top-up. */
    DEPOSIT,
    /** Money paid out of a wallet to the user's bank. */
    WITHDRAWAL,
    /** Money changed from one currency to another between the same user's wallets. */
    EXCHANGE,
    /** Money sent from one user to another. */
    TRANSFER,
    /** Money set aside from a wallet while a payment waits for approval. */
    HOLD,
    /** Held money returned to the wallet, e.g. when staff reject the payment. */
    RELEASE,
    /** A completed payment reversed by staff. */
    REFUND
}
