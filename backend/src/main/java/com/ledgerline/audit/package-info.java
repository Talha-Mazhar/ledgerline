/**
 * Audit module: an append-only log of what staff did in the back office (approvals, rejections,
 * refunds, verification reviews, role changes).
 *
 * <p>It listens to other modules' events inside the application ({@code @ApplicationModuleListener}):
 * each handler runs after the change has committed, in its own transaction, and Spring Modulith's
 * event registry retries it if it fails, so the log can't silently miss an action.
 */
@NullMarked
@ApplicationModule(
        displayName = "Audit",
        allowedDependencies = {"payments", "onboarding", "identity"})
package com.ledgerline.audit;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
