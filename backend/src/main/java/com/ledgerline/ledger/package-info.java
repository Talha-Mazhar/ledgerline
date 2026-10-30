/**
 * Ledger module: the double-entry ledger at the core of Ledgerline.
 *
 * <p>Owns ledger accounts, journal entries and postings. Every movement of money in the system is
 * recorded here as a balanced journal entry, through this module's public API. The ledger depends
 * on no other module; the others depend on it.
 */
@NullMarked
@ApplicationModule(displayName = "Ledger")
package com.ledgerline.ledger;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
