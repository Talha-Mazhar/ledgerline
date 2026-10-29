/**
 * FX module: exchange rates, quotes with an expiry, and currency conversions.
 *
 * <p>A conversion is recorded as a journal entry in the ledger. May depend on the {@code ledger}
 * module only.
 */
@NullMarked
@ApplicationModule(displayName = "Currency exchange", allowedDependencies = "ledger")
package com.ledgerline.fx;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
