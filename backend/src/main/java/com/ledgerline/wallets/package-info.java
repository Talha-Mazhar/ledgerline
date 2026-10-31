/**
 * Wallets module: the wallets users see, one per currency, each backed by a ledger account.
 *
 * <p>May depend on the {@code ledger} module (money and currencies) and the {@code identity} module
 * (who the current user is).
 *
 * <p>Everything here except the module's future public API is package-private: the controller,
 * service, entity and repository can only be used from inside this package, so other modules cannot
 * reach past the API into wallet internals. The compiler enforces that, not a convention.
 */
@NullMarked
@ApplicationModule(
        displayName = "Wallets",
        allowedDependencies = {"ledger", "identity"})
package com.ledgerline.wallets;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
