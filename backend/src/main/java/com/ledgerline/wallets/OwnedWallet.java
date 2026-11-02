package com.ledgerline.wallets;

import com.ledgerline.ledger.CurrencyCode;
import java.time.Instant;
import java.util.UUID;

/**
 * What other modules may know about a wallet. The wallet's id is also the id of its ledger account,
 * so a payment can post to {@code id} directly.
 */
public record OwnedWallet(UUID id, UUID ownerId, CurrencyCode currency, Instant openedAt) {}
