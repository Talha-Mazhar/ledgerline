/**
 * Payments module: everything that moves money: deposits, withdrawals, currency exchange and
 * transfers to other users. Each movement is one balanced journal entry in the ledger.
 *
 * <p>May depend on {@code ledger} (to post entries), {@code wallets} (to find the wallets involved),
 * {@code fx} (exchange rates) and {@code identity} (who is asking).
 */
@NullMarked
@ApplicationModule(
        displayName = "Payments",
        allowedDependencies = {"ledger", "wallets", "fx", "identity"})
package com.ledgerline.payments;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
