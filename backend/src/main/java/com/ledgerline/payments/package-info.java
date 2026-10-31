/**
 * Payments module: everything that moves money. Deposits and withdrawals now; currency exchange and
 * transfers between users later. Each movement is one balanced journal entry in the ledger.
 *
 * <p>May depend on {@code ledger} (to post entries), {@code wallets} (to find the wallets involved)
 * and {@code identity} (who is asking).
 */
@NullMarked
@ApplicationModule(
        displayName = "Payments",
        allowedDependencies = {"ledger", "wallets", "identity"})
package com.ledgerline.payments;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
