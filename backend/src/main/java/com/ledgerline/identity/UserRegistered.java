package com.ledgerline.identity;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/** A new account was created. Published to Kafka, where notifications sends the welcome. */
@Externalized("ledgerline.identity::#{#this.userId()}")
public record UserRegistered(UUID eventId, UUID userId, String email, String displayName) {}
