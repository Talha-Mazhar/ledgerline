/**
 * Messaging module: how events leave the application through Kafka. Depends on no other module.
 *
 * <ul>
 *   <li>Modules publish ordinary Spring application events. Spring Modulith stores each one in the
 *       {@code event_publication} table in the same transaction as the change that caused it (a
 *       transactional outbox) and, after commit, sends the {@code @Externalized} ones to Kafka.
 *   <li>If Kafka is down, the send fails and the publication stays incomplete. {@link
 *       com.ledgerline.messaging.OutboxResubmitter} sends it again every minute, so no event is
 *       lost; consumers are idempotent, so an event sent twice does no harm.
 * </ul>
 */
@NullMarked
@ApplicationModule(displayName = "Messaging", allowedDependencies = {})
package com.ledgerline.messaging;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
