package com.ledgerline.identity;

import java.util.UUID;

/** An admin changed a user's role. Recorded in the audit log. */
public record RoleChanged(UUID actorId, UUID userId, Role from, Role to) {}
