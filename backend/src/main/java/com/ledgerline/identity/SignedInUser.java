package com.ledgerline.identity;

import java.io.Serializable;
import java.util.UUID;

/**
 * Who is signed in: the principal Spring Security keeps in the session. It holds no password, so
 * the session never carries a credential.
 *
 * <p>Public so tests can sign a user in without a password.
 */
public record SignedInUser(UUID id, String email, String displayName) implements Serializable {}
