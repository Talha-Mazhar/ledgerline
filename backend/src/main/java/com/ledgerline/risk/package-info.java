/**
 * Risk module: decides whether a payment goes through at once or waits for staff approval.
 *
 * <p>Two kinds of rule: limits that depend on the customer's verification tier, and velocity
 * rules that catch unusual behaviour (many payments in a short time, a large first payment to a
 * new recipient). A rule never refuses a payment outright; it holds it for two staff to review.
 *
 * <p>May depend on {@code onboarding} (the customer's tier) and {@code fx} (comparing amounts in
 * different currencies in euros).
 */
@NullMarked
@ApplicationModule(
        displayName = "Risk",
        allowedDependencies = {"onboarding", "fx", "ledger", "identity"})
package com.ledgerline.risk;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
