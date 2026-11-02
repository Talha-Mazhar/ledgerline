/**
 * Payments module: everything that moves money: deposits, withdrawals, currency exchange and
 * transfers to other users. Each movement is one balanced journal entry in the ledger.
 *
 * <p>Each payment also has a row in {@code payments} with its state machine (completed, held for
 * approval, rejected, refunded), and every change publishes an event. Events are stored in the
 * same transaction as the change (Spring Modulith's event publication registry, a transactional
 * outbox) and published to Kafka from there, so none is lost if Kafka is down.
 *
 * <p>May depend on {@code ledger} (to post entries), {@code wallets} (to find the wallets involved),
 * {@code fx} (exchange rates), {@code identity} (who is asking) and {@code risk} (whether a payment
 * must wait for approval).
 */
@NullMarked
@ApplicationModule(
        displayName = "Payments",
        allowedDependencies = {"ledger", "wallets", "fx", "identity", "risk"})
package com.ledgerline.payments;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
