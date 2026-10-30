/**
 * Identity module: users, sign-in and roles.
 *
 * <p>Other modules learn about new users through events published by this module, never by
 * reading its data.
 */
@NullMarked
@ApplicationModule(displayName = "Identity")
package com.ledgerline.identity;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
