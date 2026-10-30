package com.ledgerline.identity;

import java.util.UUID;

/**
 * The user the current request acts for.
 *
 * <p>Other modules ask this interface for the user's id and never look at how sign-in works. Until
 * sign-in exists, {@link DemoCurrentUser} answers with one fixed demo user; adding Spring Security
 * replaces that class and nothing that depends on this interface changes.
 */
public interface CurrentUser {

    UUID id();
}
