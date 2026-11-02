/**
 * Notifications module: tells people what happened to their money, in the app and by email.
 *
 * <p>It doesn't call the modules that make things happen and doesn't use their classes: it reads
 * their events from Kafka, as JSON. It could run as a separate service without changing a line of
 * the other modules. May depend on {@code identity} (names and emails of the people to notify)
 * and {@code messaging} (topic names).
 */
@NullMarked
@ApplicationModule(
        displayName = "Notifications",
        allowedDependencies = {"identity", "messaging"})
package com.ledgerline.notifications;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
