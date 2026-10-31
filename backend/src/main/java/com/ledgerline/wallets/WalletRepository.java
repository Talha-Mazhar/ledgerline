package com.ledgerline.wallets;

import com.ledgerline.ledger.CurrencyCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every query takes the owner's id, so one user can never load another user's wallet by guessing
 * its id: a wallet that belongs to someone else is simply not found.
 */
interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByOwnerIdOrderByOpenedAtAsc(UUID ownerId);

    Optional<Wallet> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Wallet> findByOwnerIdAndCurrency(UUID ownerId, CurrencyCode currency);

    boolean existsByOwnerIdAndCurrency(UUID ownerId, CurrencyCode currency);
}
