package com.ledgerline.identity;

import java.util.UUID;

/** What other modules may know about a user. */
public record UserSummary(UUID id, String email, String displayName) {}
