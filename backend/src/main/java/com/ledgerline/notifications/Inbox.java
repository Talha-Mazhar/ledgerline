package com.ledgerline.notifications;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@code notifications} table. Inserting is idempotent: the (event, user) pair is unique, so
 * a Kafka message delivered twice adds the notification once.
 */
@Repository
class Inbox {

    /** One notification as the user sees it. */
    record Item(
            UUID id,
            String kind,
            String title,
            String body,
            @Nullable String link,
            Instant createdAt,
            boolean read) {}

    private final JdbcClient jdbc;
    private final Clock clock;

    Inbox(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Stores a notification; true when it is new, false when the event was already handled. */
    @Transactional
    boolean add(Notice notice) {
        return jdbc.sql("""
                        INSERT INTO notifications (id, user_id, event_id, kind, title, body, link, created_at)
                        VALUES (:id, :user, :event, :kind, :title, :body, :link, :now)
                        ON CONFLICT (event_id, user_id) DO NOTHING
                        """)
                        .param("id", UUID.randomUUID())
                        .param("user", notice.userId())
                        .param("event", notice.eventId())
                        .param("kind", notice.kind())
                        .param("title", notice.title())
                        .param("body", notice.body())
                        .param("link", notice.link())
                        .param("now", now())
                        .update()
                == 1;
    }

    @Transactional(readOnly = true)
    List<Item> latest(UUID user, int limit) {
        return jdbc.sql("""
                        SELECT id, kind, title, body, link, created_at, read_at FROM notifications
                        WHERE user_id = :user ORDER BY created_at DESC LIMIT :limit
                        """)
                .param("user", user)
                .param("limit", limit)
                .query((rs, row) -> new Item(
                        rs.getObject("id", UUID.class),
                        rs.getString("kind"),
                        rs.getString("title"),
                        rs.getString("body"),
                        rs.getString("link"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("read_at") != null))
                .list();
    }

    @Transactional(readOnly = true)
    int unread(UUID user) {
        return jdbc.sql("SELECT COUNT(*) FROM notifications WHERE user_id = :user AND read_at IS NULL")
                .param("user", user)
                .query(Integer.class)
                .single();
    }

    @Transactional
    void markRead(UUID user, UUID id) {
        jdbc.sql("UPDATE notifications SET read_at = :now WHERE id = :id AND user_id = :user AND read_at IS NULL")
                .param("now", now())
                .param("id", id)
                .param("user", user)
                .update();
    }

    @Transactional
    void markAllRead(UUID user) {
        jdbc.sql("UPDATE notifications SET read_at = :now WHERE user_id = :user AND read_at IS NULL")
                .param("now", now())
                .param("user", user)
                .update();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
