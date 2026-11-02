/**
 * Statements module: monthly PDF statements per wallet, built from the ledger.
 *
 * <p>A statement's opening balance, lines and closing balance all come from the ledger, so it
 * matches the ledger to the cent by construction; a test checks that too. On the first of each
 * month a job (on one instance, ShedLock) archives last month's statements in object storage and
 * tells each customer theirs is ready.
 *
 * <p>May depend on {@code ledger}, {@code wallets}, {@code identity} and {@code documents}.
 */
@NullMarked
@ApplicationModule(
        displayName = "Statements",
        allowedDependencies = {"ledger", "wallets", "identity", "documents"})
package com.ledgerline.statements;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
