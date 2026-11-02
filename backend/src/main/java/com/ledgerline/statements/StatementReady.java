package com.ledgerline.statements;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/** Last month's statement was archived; the customer is told it's ready. */
@Externalized("ledgerline.statements.ready")
public record StatementReady(UUID eventId, UUID userId, UUID walletId, String currency, String month) {}
