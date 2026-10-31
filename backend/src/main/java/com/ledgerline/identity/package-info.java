/**
 * Identity module: accounts, sign-in and who the current user is.
 *
 * <p>Other modules use only its public API: {@link com.ledgerline.identity.CurrentUser} (the
 * signed-in user's id) and {@link com.ledgerline.identity.UserDirectory} (finding another user,
 * e.g. the recipient of a transfer). How passwords and sessions work stays inside this package.
 */
@NullMarked
@ApplicationModule(displayName = "Identity", allowedDependencies = {})
package com.ledgerline.identity;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
