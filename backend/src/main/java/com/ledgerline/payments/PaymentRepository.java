package com.ledgerline.payments;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByInitiatedByAndIdempotencyKey(UUID initiatedBy, String idempotencyKey);

    /** Locks the row for the rest of the transaction, so two staff can't approve it at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> lockById(@Param("id") UUID id);

    List<Payment> findByStatusInOrderByCreatedAt(Collection<PaymentStatus> statuses, Pageable page);

    List<Payment> findByStatusInOrderByCreatedAtDesc(Collection<PaymentStatus> statuses, Pageable page);

    /** A customer's payments: the ones they made and the transfers they received, newest first. */
    @Query("""
            SELECT p FROM Payment p
            WHERE p.initiatedBy = :user
               OR (p.counterpartyId = :user
                   AND p.status IN (com.ledgerline.payments.PaymentStatus.COMPLETED,
                                    com.ledgerline.payments.PaymentStatus.REFUNDED))
            ORDER BY p.createdAt DESC
            """)
    List<Payment> involving(@Param("user") UUID user, Pageable page);

    /** Outgoing payments (transfers and withdrawals) since a moment, not counting rejected ones. */
    @Query("""
            SELECT p FROM Payment p
            WHERE p.initiatedBy = :user AND p.createdAt >= :since
              AND p.kind IN (com.ledgerline.payments.PaymentKind.TRANSFER, com.ledgerline.payments.PaymentKind.WITHDRAWAL)
              AND p.status <> com.ledgerline.payments.PaymentStatus.REJECTED
            """)
    List<Payment> outgoingSince(@Param("user") UUID user, @Param("since") Instant since);

    boolean existsByInitiatedByAndCounterpartyIdAndStatusIn(
            UUID initiatedBy, UUID counterpartyId, Collection<PaymentStatus> statuses);
}
