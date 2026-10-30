/**
 * Wallets module: the wallets users see, one per currency, each backed by a ledger account.
 *
 * <p>May depend on the {@code ledger} module only.
 */
@NullMarked
@ApplicationModule(displayName = "Wallets", allowedDependencies = "ledger")
package com.ledgerline.wallets;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
