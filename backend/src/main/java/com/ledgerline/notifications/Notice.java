package com.ledgerline.notifications;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A notification to deliver: in the app always, by email when {@code email} is set. */
record Notice(UUID userId, UUID eventId, String kind, String title, String body, @Nullable String link) {}
