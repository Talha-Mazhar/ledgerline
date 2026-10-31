package com.ledgerline.identity;

import java.util.UUID;

/**
 * The user the current request acts for.
 *
 * <p>Other modules ask this interface for the user's id and never look at how sign-in works. It is
 * answered by {@link SecurityCurrentUser}, which reads the signed-in user from Spring Security.
 */
public interface CurrentUser {

    UUID id();
}
