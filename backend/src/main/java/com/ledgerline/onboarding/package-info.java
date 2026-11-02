/**
 * Onboarding module: identity verification (KYC). Customers upload an ID document, support staff
 * approve or reject it, and verified customers move to a higher tier with higher limits.
 *
 * <p>Public API: {@link com.ledgerline.onboarding.CustomerTiers} (a customer's tier) and the
 * events it publishes. May depend on {@code identity} (who is asking, user names) and {@code
 * documents} (where the uploaded file is kept).
 */
@NullMarked
@ApplicationModule(
        displayName = "Onboarding",
        allowedDependencies = {"identity", "documents"})
package com.ledgerline.onboarding;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
