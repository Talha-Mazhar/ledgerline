/**
 * Payments module: top-ups from the payment provider and transfers between wallets.
 *
 * <p>Turns each payment into a journal entry in the ledger. May depend on the {@code ledger} and
 * {@code wallets} modules.
 */
@NullMarked
@ApplicationModule(displayName = "Payments", allowedDependencies = {"ledger", "wallets"})
package com.ledgerline.payments;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
